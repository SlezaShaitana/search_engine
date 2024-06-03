package searchengine.services.indexing;


public interface WebSiteIndexingService {

    void startIndexing();

    void stopIndexing();

    void addOrUpdate(String url);
}
