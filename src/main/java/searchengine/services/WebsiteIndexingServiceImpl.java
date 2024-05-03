package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.IndexationStatuses;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Service
@Slf4j
public class WebsiteIndexingServiceImpl implements WebSiteIndexingService<SiteDto> {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private final AtomicBoolean stopIndexingFlag;
    private ForkJoinPool pool;
    @Getter
    private boolean isIndexing;

    public boolean existenceSiteInConfigurationFile(String url) {
        List<Site> sitesList = sites.getSites();
        return sitesList.stream()
                .anyMatch(site -> url.equals(site.getUrl()));
    }

    public SiteEntity rebuildingCreatingInDatabase(Site site) {
        SiteEntity siteEntityDelete = siteRepository.findByUrl(site.getUrl());
        if (siteEntityDelete != null) {
            siteRepository.delete(siteEntityDelete);
        }
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setStatus(IndexationStatuses.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError(null);
        siteEntity.setUrl(site.getUrl());
        siteRepository.save(siteEntity);
        return siteEntity;
    }


    public void startIndexing() {
        pool = new ForkJoinPool();
        stopIndexingFlag.set(false);
        isIndexing = true;
        List<Site> sitesList = sites.getSites();
        List<ForkJoinTask<Void>> forkJoinTaskList = new ArrayList<>();

        for (Site site : sitesList) {
            SiteEntity siteEntity = rebuildingCreatingInDatabase(site);
            ForkJoinTask<Void> forkJoinTask = new IndexingRecursiveAction(
                    site.getUrl(), siteEntity, 4, 0,
                    pageRepository, siteRepository, stopIndexingFlag, pool);
            forkJoinTaskList.add(forkJoinTask);
            forkJoinTask.fork();
        }
        for (ForkJoinTask<Void> task : forkJoinTaskList) {
            task.join();
        }
        pool.shutdown();
    }

    @Override
    public void stopIndexing() {
        log.info("The user stopped indexing");
        stopIndexingFlag.set(true);
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
            try {
                pool.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.interrupted();
                Thread.currentThread().interrupt();
            }
        }
        if (isIndexing) {
            List<SiteEntity> sites = siteRepository.findAll();
            for (SiteEntity site : sites) {
                site.setStatus(IndexationStatuses.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
            isIndexing = false;
        }
    }

    @Override
    public void addOrUpdate(String url) {
        pool = new ForkJoinPool();
        stopIndexingFlag.set(false);
        if (existenceSiteInConfigurationFile(url)) {
            isIndexing = true;
            List<Site> sitesList = sites.getSites();
            Site site = sitesList.stream()
                    .filter(s -> url.equals(s.getUrl()))
                    .findFirst()
                    .get();

            SiteEntity siteEntity = rebuildingCreatingInDatabase(site);
            pool.invoke(new IndexingRecursiveAction(site.getUrl(), siteEntity, 4, 0,
                    pageRepository, siteRepository, stopIndexingFlag, pool));
            pool.shutdown();
            try {
                pool.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (pool.isTerminated()) {
                isIndexing = false;
            }
        }
    }

}
