package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.search.SearchData;

import java.util.List;

public interface SearchService {
    ResponseEntity<Object> search(String query, String site, int offset, int limit);

    List<SearchData> searchThroughAllSites(String query, int offset);

    List<SearchData> onePageSearch(String query, String url, int offset);
}