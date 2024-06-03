package searchengine.services.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Component
public class Lemmatizer {
    private final RussianLuceneMorphology russianLuceneMorphology;

    public Lemmatizer() {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String stripHtmlTags(String html) {
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(html);
        return matcher.replaceAll("");
    }

    public String normalizeText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim();
    }

    public HashMap<String, Integer> getLemmaCounts(String text) {
        text = normalizeText(text);
        text = stripHtmlTags(text);
        HashMap<String, Integer> lemmaCounts = new HashMap<>();
        String[] words = text.toLowerCase(Locale.ROOT).split("\\s+");
        for (String w : words) {
            List<String> lemmas = getLemmaList(w);
            if (!lemmas.isEmpty()) {
                for (String lemma : lemmas) {
                    int count = lemmaCounts.getOrDefault(lemma, 0);
                    lemmaCounts.put(lemma, count + 1);
                }
            }
        }
        return lemmaCounts;
    }

    private boolean isRussianStopWord(String word) {
        List<String> morphForm = russianLuceneMorphology.getMorphInfo(word);
        return morphForm.stream()
                .anyMatch(element -> element.contains("ПРЕДЛ")
                        || element.contains("СОЮЗ")
                        || element.contains("МЕЖД")
                        || element.contains("МС")
                        || element.contains("ЧАСТ")
                        || element.length() <= 3);
    }

    private boolean isWordInRussian(String word) {
        String regex = "[а-яА-Я]+";
        return word.matches(regex);
    }

    public List<String> getLemmaList(String word) {
        List<String> wordLemmas = new ArrayList<>();
        try {
            if (isWordInRussian(word)) {
                List<String> normalForms = russianLuceneMorphology.getNormalForms(word);
                if (!isRussianStopWord(word) && !word.isEmpty()) {
                    wordLemmas.addAll(normalForms);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return wordLemmas;
    }

    public List<Integer> locateLemmasInText(String htmlPage, List<String> targetLemmas) {
        return targetLemmas.stream()
                .flatMap(lemma -> locateLemmaInText(htmlPage, lemma).stream())
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Integer> locateLemmaInText(String htmlPage, String targetLemma) {
        List<Integer> lemmaOccurrences = new ArrayList<>();
        String[] words = htmlPage.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int currentIndex = 0;
        for (String word : words) {
            List<String> wordLemmas = getLemmaList(word);
            for (String currentLemma : wordLemmas) {
                if (currentLemma.equals(targetLemma)) {
                    lemmaOccurrences.add(currentIndex);
                }
            }
            currentIndex += word.length() + 1;
        }
        return lemmaOccurrences;
    }
}
