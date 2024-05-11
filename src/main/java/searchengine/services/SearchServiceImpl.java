package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.repository.LemmaRepository;

import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;

    @Override
    public HashMap<String, Integer> morphologyForms(String word) {
        return null;
    }

    @Override
    public ResponseEntity<Object> search(String query, String site, int offset, int limit) {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
