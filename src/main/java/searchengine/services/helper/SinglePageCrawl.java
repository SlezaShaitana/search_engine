package searchengine.services.helper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import searchengine.model.*;

import java.io.IOException;

@Slf4j
public class SinglePageCrawl {
    private Lemmatizer lemmatizer = new Lemmatizer();
    private final int idSite;
    private final String path;
    private final EntityFactory entityFactory;
    private final ConnectToPage connectToPage;

    public SinglePageCrawl(int idSite, String path,
                           EntityFactory entityFactory, ConnectToPage connectToPage) {
        this.idSite = idSite;
        this.path = path;
        this.entityFactory = entityFactory;
        this.connectToPage = connectToPage;
    }

    public void indexPage(String url) {
        try {
            log.info("Page update {}", url);
            Connection connection = connectToPage.connectToPage(url);
            Document page = connection.get();
            String pageContent = page.toString();
            int statusCode = connection.response().statusCode();
            SiteEntity siteEntity = entityFactory.findById(idSite);
            siteEntity.setStatus(IndexationStatuses.INDEXING);
            siteEntity.setLastError(null);
            entityFactory.savingToSiteRepository(siteEntity);
            PageEntity pageEntity = entityFactory.createPageEntity(siteEntity, path, pageContent, statusCode);
            entityFactory.handleLemmas(lemmatizer, pageContent, siteEntity, pageEntity);
            siteEntity.setStatus(IndexationStatuses.INDEXED);
            entityFactory.savingToSiteRepository(siteEntity);
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}