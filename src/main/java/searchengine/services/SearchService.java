package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.search.SearchData;

import java.util.List;

public interface SearchService {
    ResponseEntity<Object> search(String query, String site, int offset, int limit);

    List<SearchData> searchOnAllSites(List<String> lemmasFromQuery);

    List<SearchData> onePageSearch(List<String> lemmasFromQuery, String url);
}