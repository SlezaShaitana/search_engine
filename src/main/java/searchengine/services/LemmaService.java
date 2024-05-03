package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.repository.LemmaRepository;

@RequiredArgsConstructor
@Service
public class LemmaService {
    private final LemmaRepository lemmaRepository;
}
