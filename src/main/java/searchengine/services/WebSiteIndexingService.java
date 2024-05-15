package searchengine.services;

public interface WebSiteIndexingService {

    void startIndexing();
    void stopIndexing();
    void addOrUpdate(String url);
}
