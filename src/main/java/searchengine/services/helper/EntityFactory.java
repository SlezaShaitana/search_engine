package searchengine.services.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class EntityFactory {

    private static EntityFactory instance;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Autowired
    public EntityFactory(LemmaRepository lemmaRepository, PageRepository pageRepository,
                         IndexRepository indexRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        instance = this;
    }

    public static EntityFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EntityFactory has not been initialized yet");
        }
        return instance;
    }

    public SiteEntity createSiteEntity(Site site) {
        log.info("Save site: {}", site.getName());
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setStatus(IndexationStatuses.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError(null);
        siteEntity.setUrl(site.getUrl());
        siteRepository.save(siteEntity);
        return siteEntity;
    }

    public LemmaEntity createLemmaEntity(SiteEntity siteEntity, String lemma) {
        log.info("Save lemma: {}", lemma);
        LemmaEntity newLemma = new LemmaEntity();
        newLemma.setSites(siteEntity);
        newLemma.setLemma(lemma);
        newLemma.setFrequency(1);
        lemmaRepository.save(newLemma);
        return newLemma;
    }

    public PageEntity createPageEntity(SiteEntity siteEntity, String childUrl, String content, int statusCode) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSites(siteEntity);
        pageEntity.setPath(childUrl);
        pageEntity.setContent(content);
        pageEntity.setCode(statusCode);
        log.info("Save page: {}", childUrl);
        pageRepository.save(pageEntity);
        return pageEntity;
    }

    public void createIndexEntity(PageEntity pageEntity, LemmaEntity newLemma, float count) {
        log.info("Save index");
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(pageEntity);
        indexEntity.setLemmaId(newLemma);
        indexEntity.setRank(count);
        indexRepository.save(indexEntity);
    }

    public void savingToLemmaRepository(LemmaEntity lemmaEntity) {
        lemmaRepository.save(lemmaEntity);
    }

    public void savingToSiteRepository(SiteEntity siteEntity) {
        siteRepository.save(siteEntity);
    }

    public void savingToIndexRepository(IndexEntity indexEntity) {
        indexRepository.save(indexEntity);
    }

    public boolean existByPath(String childUrl) {
        return pageRepository.existsByPath(childUrl);
    }

    public LemmaEntity findByLemmaIgnoreCaseAndSitesId(Map.Entry<String, Integer> lemma, SiteEntity siteEntity) {
        return lemmaRepository.findByLemmaIgnoreCaseAndSitesId(lemma.getKey(), siteEntity.getId());
    }

    public IndexEntity findByLemmaIdAndPageId(LemmaEntity lemmaEntity, PageEntity pageEntity) {
        return indexRepository.findByLemmaIdAndPageId(lemmaEntity, pageEntity);
    }

    public SiteEntity findById(Integer idSite) {
        return siteRepository.findById(idSite).orElseThrow(() -> new RuntimeException("Site not found"));
    }

    public void handleLemmas(LemmaFinder lemmaFinder, String content, SiteEntity siteEntity, PageEntity pageEntity) {
        HashMap<String, Integer> lemmasList = lemmaFinder.collectLemmas(content);
        for (Map.Entry<String, Integer> lemma : lemmasList.entrySet()) {
            float count = lemma.getValue();
            LemmaEntity lemmaEntity = findByLemmaIgnoreCaseAndSitesId(lemma, siteEntity);
            IndexEntity indexEntityUniquePage = findByLemmaIdAndPageId(lemmaEntity, pageEntity);
            if (lemmaEntity != null) {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                savingToLemmaRepository(lemmaEntity);
                if (indexEntityUniquePage != null) {
                    indexEntityUniquePage.setRank(count);
                    savingToIndexRepository(indexEntityUniquePage);
                }
                if (indexEntityUniquePage == null) {
                    createIndexEntity(pageEntity, lemmaEntity, count);
                }
            } else {
                LemmaEntity newLemma = createLemmaEntity(siteEntity, lemma.getKey());
                createIndexEntity(pageEntity, newLemma, count);
            }
        }
    }
}
