package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.search.SearchData;

import java.util.List;

public interface SearchService {
    ResponseEntity<Object> searchByQueryAndUrlWithPagination(String query, String site, int offset, int limit);

    List<SearchData> searchOnAllSites(List<String> lemmasFromQuery);

    List<SearchData> searchOnOnePage(List<String> lemmasFromQuery, String url);
}