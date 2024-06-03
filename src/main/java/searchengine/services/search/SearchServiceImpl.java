package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.EmptyQueryException;
import searchengine.exceptions.SearchDataNotFoundException;
import searchengine.exceptions.SiteUrlNotAllowedException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.helper.ConnectToPage;
import searchengine.services.helper.Lemmatizer;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Lemmatizer lemmatizer;
    private final ConnectToPage connectToPage;
    private int countWordsRequest;
    private static final int MAX_SNIPPET_LENGTH = 400;
    private static final int FRAGMENT_LENGTH = 150;
    private List<SearchData> searchResults = new ArrayList<>();
    private int resultCount;

    @Override
    public ResponseEntity<Object> searchByQueryAndUrlWithPagination(String query, String url, int offset, int limit) {
        if (query.isEmpty()) {
            throw new EmptyQueryException("Задан пустой поисковый запрос");
        } else {
            List<String> lemmasFromQuery = convertQueryToLemmas(query);
            String[] words = query.split("\\s+");
            countWordsRequest = words.length;
            if (offset == 0) {
                searchResults.clear();
            }
            performPageSearch(url, lemmasFromQuery);
            List<SearchData> searchDataSublist = searchResults.subList(offset, Math.min(offset + limit, searchResults.size()));
            return new ResponseEntity<>(new SearchResponse(true, resultCount, searchDataSublist), HttpStatus.OK);
        }
    }

    private void performPageSearch(String url, List<String> lemmasFromQuery) {
        if (searchResults.isEmpty()) {
            List<SearchData> resultSearchData = determineSearchScope(url, lemmasFromQuery);
            searchResults = resultSearchData;
            resultCount = resultSearchData.size();
        }
    }

    private List<SearchData> determineSearchScope(String url, List<String> lemmasFromQuery) {
        List<SearchData> searchData;
        if (!url.isEmpty()) {
            if (siteRepository.findByUrl(url) == null) {
                throw new SiteUrlNotAllowedException("Указанная страница не найдена");
            } else {
                searchData = searchOnOnePage(lemmasFromQuery, url);
            }
        } else {
            searchData = searchOnAllSites(lemmasFromQuery);
        }
        if (searchData == null) {
            throw new SearchDataNotFoundException("NOT_FOUND");
        }
        return searchData;
    }

    @Override
    public List<SearchData> searchOnAllSites(List<String> lemmasFromQuery) {
        List<SiteEntity> sites = siteRepository.findAll();
        List<LemmaEntity> sortedLemmasPerSite = new ArrayList<>();
        for (SiteEntity siteEntity : sites) {
            sortedLemmasPerSite.addAll(getLemmasFromSiteSortedByFrequency(lemmasFromQuery, siteEntity));
        }
        List<SearchData> searchData = null;
        for (LemmaEntity lemmaEntity : sortedLemmasPerSite) {
            if (lemmasFromQuery.contains(lemmaEntity.getLemma())) {
                searchData = new ArrayList<>(createSearchDataListFromLemmas(sortedLemmasPerSite, lemmasFromQuery));
                searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
            }
        }
        return searchData;
    }

    @Override
    public List<SearchData> searchOnOnePage(List<String> lemmasFromQuery, String url) {
        SiteEntity siteEntity = siteRepository.findByUrl(url);
        List<LemmaEntity> lemmasFromSite = getLemmasFromSiteSortedByFrequency(lemmasFromQuery, siteEntity);
        return createSearchDataListFromLemmas(lemmasFromSite, lemmasFromQuery);
    }

    private List<String> convertQueryToLemmas(String query) {
        log.info("Get query into lemma");
        query = lemmatizer.normalizeText(query);
        String[] words = query.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String word : words) {
            List<String> lemma = lemmatizer.getLemmaList(word);
            lemmaList.addAll(lemma);
        }
        return lemmaList;
    }

    private List<LemmaEntity> getLemmasFromSiteSortedByFrequency(List<String> lemmas, SiteEntity site) {
        log.info("Get lemmas from site: {}", site.getUrl());
        ArrayList<LemmaEntity> lemmaList = (ArrayList<LemmaEntity>) lemmaRepository.findLemmasBySite(lemmas, site.getId());
        lemmaList.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return lemmaList;
    }

    private List<SearchData> createSearchDataListFromLemmas(List<LemmaEntity> lemmas, List<String> lemmasFromQuery) {
        List<SearchData> searchDataList = new ArrayList<>();
        List<Integer> lemmaIds = lemmas.stream().map(LemmaEntity::getId).toList();
        if (lemmas.size() >= lemmasFromQuery.size()) {
            List<PageEntity> sortedPageList = pageRepository.findByLemmas(lemmaIds, lemmasFromQuery.size());
            List<Integer> pageIds = sortedPageList.stream().map(PageEntity::getId).toList();
            List<IndexEntity> sortedIndexList = indexRepository.findByLemmasAndPages(lemmaIds, pageIds);
            LinkedHashMap<PageEntity, Float> sortedPagesByAbsRelevance =
                    getPagesSortedByAbsoluteRelevance(sortedPageList, sortedIndexList);
            searchDataList = createSearchDataListFromSortedPages(sortedPagesByAbsRelevance, lemmasFromQuery);

            return searchDataList;
        } else return searchDataList;
    }

    private LinkedHashMap<PageEntity, Float> getPagesSortedByAbsoluteRelevance(List<PageEntity> pages,
                                                                               List<IndexEntity> indexes) {
        HashMap<PageEntity, Float> pageToRelevance = new HashMap<>();
        for (PageEntity page : pages) {
            float relevant = 0;
            for (IndexEntity index : indexes) {
                if (index.getPageId().equals(page)) {
                    relevant += index.getRank();
                }
            }
            pageToRelevance.put(page, relevant);
        }
        HashMap<PageEntity, Float> pageToAbsRelevance = new HashMap<>();
        for (PageEntity page : pageToRelevance.keySet()) {
            float absRelevance = pageToRelevance.get(page) / Collections.max(pageToRelevance.values());
            pageToAbsRelevance.put(page, absRelevance);
        }
        return pageToAbsRelevance
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<SearchData> createSearchDataListFromSortedPages(LinkedHashMap<PageEntity, Float> sortedPages,
                                                                 List<String> lemmasFromQuery) {
        List<SearchData> searchData = new ArrayList<>();
        for (PageEntity pageEntity : sortedPages.keySet()) {
            SiteEntity siteEntity = pageEntity.getSites();
            String site = siteEntity.getUrl();
            String uri = pageEntity.getPath();
            String content = pageEntity.getContent();
            String title = connectToPage.getTitleFromHtml(content);
            String siteName = siteEntity.getName();
            Float absRelevance = sortedPages.get(pageEntity);
            String clearContent = lemmatizer.stripHtmlTags(content);
            String snippet = createSnippetFromContent(clearContent, lemmasFromQuery);
            searchData.add(new SearchData(site, siteName, uri, title, snippet, absRelevance));
        }
        return searchData;
    }

    private String createSnippetFromContent(String content, List<String> lemmasFromQuery) {
        List<Integer> lemmaIndexes = lemmatizer.locateLemmasInText(content, lemmasFromQuery);
        List<String> highlightedWords = getHighlightedFragmentsByLemmaIndices(content, lemmaIndexes);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < highlightedWords.size() && result.length() < MAX_SNIPPET_LENGTH; i++) {
            String word = highlightedWords.get(i);
            if (result.length() + word.length() < MAX_SNIPPET_LENGTH) {
                result.append(word).append("... ");
            } else {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getHighlightedFragmentsByLemmaIndices(String content, List<Integer> lemmaIndexes) {
        List<String> highlightedLemmaFragments = new ArrayList<>();
        for (int i = 0; i < lemmaIndexes.size(); i++) {
            int lemmaStartIndex = lemmaIndexes.get(i);
            int lemmaEndIndex = content.indexOf(" ", lemmaStartIndex);
            int nextLemmaIndex = i + 1;
            while (nextLemmaIndex < lemmaIndexes.size() && lemmaIndexes.get(nextLemmaIndex) - lemmaEndIndex > 0 &&
                    lemmaIndexes.get(nextLemmaIndex) - lemmaEndIndex < countWordsRequest) {
                lemmaEndIndex = content.indexOf(" ", lemmaIndexes.get(nextLemmaIndex));
                nextLemmaIndex += 1;
            }
            i = nextLemmaIndex - 1;
            String highlightedLemmaFragment = getHighlightedWordInFragment(lemmaStartIndex, lemmaEndIndex, content);
            highlightedLemmaFragments.add(highlightedLemmaFragment);
        }
        highlightedLemmaFragments.sort(Comparator.comparingInt(String::length).reversed());
        return highlightedLemmaFragments;
    }

    private String getHighlightedWordInFragment(int lemmaStartIndex, int lemmaEndIndex, String content) {
        String word = content.substring(lemmaStartIndex, lemmaEndIndex);
        int fragmentStartIndex = content.lastIndexOf(" ", lemmaStartIndex) != -1
                ? content.lastIndexOf(" ", lemmaStartIndex) : lemmaStartIndex;
        int fragmentEndIndex = content.indexOf(" ", lemmaEndIndex + FRAGMENT_LENGTH) != -1
                ? content.indexOf(" ", lemmaEndIndex + FRAGMENT_LENGTH) : content.indexOf(" ", lemmaEndIndex);
        String text = content.substring(fragmentStartIndex, fragmentEndIndex);
        text = text.replaceAll(word, "<b>" + word + "</b>");
        return text;
    }
}
