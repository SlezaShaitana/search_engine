package searchengine.services;

public interface WebSiteIndexingService<T> {

    void startIndexing();
    void stopIndexing();
    void addOrUpdate(String url);
}
