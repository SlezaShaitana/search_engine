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

@Component
@Slf4j
public class EntityFactory {
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
    }

    public EntityFactory(LemmaRepository lemmaRepository, PageRepository pageRepository,
                         IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = null;
    }

    public EntityFactory(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = null;
        this.pageRepository = null;
        this.indexRepository = null;
    }


    public SiteEntity createSiteEntity(Site site) {
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
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(pageEntity);
        indexEntity.setLemmaId(newLemma);
        indexEntity.setRank(count);
        indexRepository.save(indexEntity);
    }
}
