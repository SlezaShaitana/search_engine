package searchengine.services.helper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SinglePageCrawl {
    private final PageRepository pageRepository;
    private LemmaFinder lemmaFinder = new LemmaFinder();
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final int idSite;
    private final SiteEntity siteEntity;
    private final String path;
    private final EntityFactory entityFactory;
    private final ConnectToPage connectToPage;

    public SinglePageCrawl(PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, int idSite, SiteEntity siteEntity, String path) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.idSite = idSite;
        this.siteEntity = siteEntity;
        this.path = path;
        entityFactory = new EntityFactory(lemmaRepository, pageRepository, indexRepository);
        connectToPage = new ConnectToPage();
    }

    public void indexPage(String url) {
        try {
            log.info("Page update {}", url);
            Connection connection = connectToPage.connectToPage(url);
            Document page = connection.get();
            String pageContent = page.toString();
            int statusCode = connection.response().statusCode();

            SiteEntity siteEntity = siteRepository.findById(idSite).orElseThrow(() -> new RuntimeException("Site not found"));
            siteEntity.setStatus(IndexationStatuses.INDEXING);
            siteEntity.setLastError(null);
            siteRepository.save(siteEntity);
            PageEntity pageEntity = entityFactory.createPageEntity(siteEntity, path, pageContent, statusCode);
            HashMap<String, Integer> lemmasList = lemmaFinder.collectLemmas(pageContent);
            for (Map.Entry<String, Integer> lemma : lemmasList.entrySet()) {
                float count = lemma.getValue();
                LemmaEntity lemmaEntity = lemmaRepository.findByLemmaIgnoreCaseAndSitesId(lemma.getKey(), siteEntity.getId());
                IndexEntity indexEntity = indexRepository.findByLemmaIdAndPageId(lemmaEntity, pageEntity);
                if (lemmaEntity != null) {
                    if (indexEntity != null) {
                        indexEntity.setRank(count);
                        indexRepository.save(indexEntity);
                    }
                    if (indexEntity == null) {
                        entityFactory.createIndexEntity(pageEntity, lemmaEntity, count);
                    }
                } else {
                    LemmaEntity newLemma = entityFactory.createLemmaEntity(siteEntity, lemma.getKey());
                    entityFactory.createIndexEntity(pageEntity, newLemma, count);
                }
                siteEntity.setStatus(IndexationStatuses.INDEXED);
                siteRepository.save(siteEntity);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}