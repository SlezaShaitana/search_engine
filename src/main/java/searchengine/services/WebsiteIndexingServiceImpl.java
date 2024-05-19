package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.helper.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Service
@Slf4j
public class WebsiteIndexingServiceImpl implements WebSiteIndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private final AtomicBoolean stopIndexingFlag;
    private ForkJoinPool pool;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Getter
    private boolean isIndexing;
    private final EntityFactory entityFactory;
    private final ConnectToPage connectToPage;
    private final LemmaFinder lemmaFinder;

    public boolean existenceSiteInConfigurationFile(String url) {
        List<Site> sitesList = sites.getSites();
        return sitesList.stream()
                .anyMatch(site -> url.startsWith(site.getUrl()));
    }

    @Transactional
    public SiteEntity rebuildingCreatingInDatabase(Site site) {
        SiteEntity siteEntityDelete = siteRepository.findByUrl(site.getUrl());
        if (siteEntityDelete != null) {
            List<Integer> pageIds = pageRepository.findBySites_Id(siteEntityDelete.getId()).stream()
                    .map(PageEntity::getId)
                    .toList();
            if (!pageIds.isEmpty()) {
                List<PageEntity> pages = pageRepository.findBySites_Id(siteEntityDelete.getId());
                for (PageEntity page : pages) {
                    List<IndexEntity> indexes = indexRepository.findByPageId_Id(page.getId());
                    indexRepository.deleteAll(indexes);
                    List<LemmaEntity> lemmas = lemmaRepository.findBySitesId(siteEntityDelete.getId());
                    lemmaRepository.deleteAll(lemmas);
                    pageRepository.delete(page);
                }
            }
            siteRepository.delete(siteEntityDelete);
        }
        return entityFactory.createSiteEntity(site);
    }

    @Async
    public void startIndexing() {
        pool = new ForkJoinPool(4);
        stopIndexingFlag.set(false);
        isIndexing = true;
        List<Site> sitesList = sites.getSites();
        List<ForkJoinTask<Void>> forkJoinTaskList = new ArrayList<>();

        for (Site site : sitesList) {
            SiteEntity siteEntity = rebuildingCreatingInDatabase(site);
            ForkJoinTask<Void> forkJoinTask = new IndexingRecursiveAction(
                    site.getUrl(), siteEntity, 4, 0, entityFactory,
                    stopIndexingFlag, pool, connectToPage, lemmaFinder);
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
                log.error(e.getMessage());
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

    @Transactional
    @Override
    @Async
    public void addOrUpdate(String url) {
        if (existenceSiteInConfigurationFile(url)) {
            String path = url.replaceFirst("https?://[^/]+", "");
            PageEntity page = pageRepository.findByPath(path);
            int idSite;
            SiteEntity siteEntity;
            if (page != null) {
                idSite = page.getSites().getId();
                siteEntity = siteRepository.getReferenceById(idSite);
                deletePage(path, page);
            } else {
                List<Site> sitesList = sites.getSites();
                Site desiredSite = sitesList.stream()
                        .filter(site -> url.startsWith(site.getUrl()))
                        .findFirst()
                        .orElse(null);

                siteEntity = entityFactory.createSiteEntity(desiredSite);
                idSite = siteEntity.getId();
            }
            SinglePageCrawl pageCrawl = new SinglePageCrawl(idSite, path, entityFactory, connectToPage);
            if (page == null || !pageRepository.existsById(page.getId())) {
                pageCrawl.indexPage(url);
            } else {
                System.out.println("Страница не была удалена");
            }
        }
    }

    @Transactional
    public void deletePage(String path, PageEntity page) {
        if (pageRepository.existsByPath(path)) {
            log.info("Delete page " + path);
            List<IndexEntity> indexesToDelete = indexRepository.findByPageId_Id(page.getId());
            for (IndexEntity index : indexesToDelete) {
                LemmaEntity lemma = index.getLemmaId();
                indexRepository.deleteById(index.getId());

                //удаление лемм которые не используются в других индексах
                if (indexRepository.countByLemmaIdAndPageId_IdIsNot(lemma, page.getId()) == 0) {
                    lemmaRepository.deleteById(lemma.getId());
                }
            }
            pageRepository.delete(page);
        }
    }
}




