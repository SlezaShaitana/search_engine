package searchengine.services;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;

public interface SearchService {
    HashMap<String, Integer> morphologyForms(String word);

    ResponseEntity<Object> search(String query, String site, int offset, int limit);

//    SaveWordResponse saveWord(String word);
//
//    WordsListResponse searchWords(SearchWordRequest searchWordRequest);
}