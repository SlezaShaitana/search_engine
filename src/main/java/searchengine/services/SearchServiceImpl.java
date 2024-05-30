package searchengine.services;

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
import searchengine.services.helper.LemmaFinder;

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
    private final LemmaFinder lemmaFinder;
    private final ConnectToPage connectToPage;
    private int countWordsRequest;
    private static final int MAX_SNIPPET_LENGTH = 350;

    @Override
    public ResponseEntity<Object> search(String query, String url, int offset, int limit) {
        if (query.isEmpty()) {
            throw new EmptyQueryException("Задан пустой поисковый запрос");
        } else {
            String[] words = query.split("\\s+");
            countWordsRequest = words.length;
            List<String> lemmasFromQuery = getQueryIntoLemma(query);
            List<SearchData> resultSearchData = determineSearchScope(url, lemmasFromQuery);
            List<SearchData> searchDataSublist = resultSearchData.subList(offset, Math.min(offset + limit, resultSearchData.size()));
            return new ResponseEntity<>(new SearchResponse(true, resultSearchData.size(), searchDataSublist), HttpStatus.OK);
        }
    }

    private List<SearchData> determineSearchScope(String url, List<String> lemmasFromQuery) {
        List<SearchData> searchData;
        if (!url.isEmpty()) {
            if (siteRepository.findByUrl(url) == null) {
                throw new SiteUrlNotAllowedException("Указанная страница не найдена");
            } else {
                searchData = onePageSearch(lemmasFromQuery, url);
            }
        } else {
            searchData = searchOnAllSites(lemmasFromQuery);
        }
        if (searchData == null) {
            throw new SearchDataNotFoundException("NOT_FOUND");
        }
        return searchData;
    }

    private List<String> getQueryIntoLemma(String query) {
        log.info("Get query into lemma");
        String[] words = query.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String word : words) {
            List<String> lemma = lemmaFinder.getLemmaList(word);
            lemmaList.addAll(lemma);
        }
        return lemmaList;
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
                searchData = new ArrayList<>(getSearchDataList(sortedLemmasPerSite, lemmasFromQuery));
                searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
            }
        }
        return searchData;
    }

    @Override
    public List<SearchData> onePageSearch(List<String> lemmasFromQuery, String url) {
        SiteEntity siteEntity = siteRepository.findByUrl(url);
        List<LemmaEntity> lemmasFromSite = getLemmasFromSiteSortedByFrequency(lemmasFromQuery, siteEntity);
        return getSearchDataList(lemmasFromSite, lemmasFromQuery);
    }

    private List<LemmaEntity> getLemmasFromSiteSortedByFrequency(List<String> lemmas, SiteEntity site) {
        log.info("Get lemmas from site: {}", site.getUrl());
        ArrayList<LemmaEntity> lemmaList = (ArrayList<LemmaEntity>) lemmaRepository.findLemmasBySite(lemmas, site.getId());
        lemmaList.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return lemmaList;
    }

    private List<SearchData> getSearchDataList(List<LemmaEntity> lemmas, List<String> lemmasFromQuery) {
        List<SearchData> searchDataList = new ArrayList<>();
        List<Integer> lemmaIds = lemmas.stream().map(LemmaEntity::getId).toList();
        if (lemmas.size() >= lemmasFromQuery.size()) {
            List<PageEntity> sortedPageList = pageRepository.findByLemmas(lemmaIds, lemmasFromQuery.size());
            List<Integer> pageIds = sortedPageList.stream().map(PageEntity::getId).toList();
            List<IndexEntity> sortedIndexList = indexRepository.findByLemmasAndPages(lemmaIds, pageIds);
            LinkedHashMap<PageEntity, Float> sortedPagesByAbsRelevance =
                    getSortPagesWithAbsRelevance(sortedPageList, sortedIndexList);
            searchDataList = getSearchData(sortedPagesByAbsRelevance, lemmasFromQuery);

            return searchDataList;
        } else return searchDataList;
    }

    private LinkedHashMap<PageEntity, Float> getSortPagesWithAbsRelevance(List<PageEntity> pages,
                                                                          List<IndexEntity> indexes) {
        HashMap<PageEntity, Float> pageWithRelevance = new HashMap<>();
        for (PageEntity page : pages) {
            float relevant = 0;
            for (IndexEntity index : indexes) {
                if (index.getPageId().equals(page)) {
                    relevant += index.getRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<PageEntity, Float> pagesWithAbsRelevance = new HashMap<>();
        for (PageEntity page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pagesWithAbsRelevance.put(page, absRelevant);
        }
        return pagesWithAbsRelevance
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<SearchData> getSearchData(LinkedHashMap<PageEntity, Float> sortedPages,
                                           List<String> lemmasFromQuey) {
        List<SearchData> searchData = new ArrayList<>();
        for (PageEntity pageEntity : sortedPages.keySet()) {
            SiteEntity siteEntity = pageEntity.getSites();
            String site = siteEntity.getUrl();
            String uri = pageEntity.getPath();
            String content = pageEntity.getContent();
            String title = connectToPage.getTitleFromHtml(content) + " - " + site + uri;
            String siteName = siteEntity.getName();
            Float absRelevance = sortedPages.get(pageEntity);
            String clearContent = lemmaFinder.removeHtmlTags(content);
            String snippet = getSnippet(clearContent, lemmasFromQuey);
            searchData.add(new SearchData(site, siteName, uri, title, snippet, absRelevance));
        }
        return searchData;
    }

    private String getSnippet(String content, List<String> lemmasFromQuey) {
        List<Integer> lemmaIndexes = findLemmaIndexesInText(content, lemmasFromQuey);
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

    private List<Integer> findLemmaIndexesInText(String content, List<String> lemmas) {
        List<Integer> indexes = new ArrayList<>();
        for (String lemma : lemmas) {
            indexes.addAll(lemmaFinder.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(indexes);
        return indexes;
    }

    private List<String> getHighlightedFragmentsByLemmaIndices(String content, List<Integer> lemmaIndexes) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndexes.size(); i++) {
            int start = lemmaIndexes.get(i);
            int end = content.indexOf(" ", start);
            int step = i + 1;
            while (step < lemmaIndexes.size() && lemmaIndexes.get(step) - end > 0 &&
                    lemmaIndexes.get(step) - end < countWordsRequest) {
                end = content.indexOf(" ", lemmaIndexes.get(step));
                step += 1;
            }
            i = step - 1;
            String text = getHighlightedWordInFragment(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getHighlightedWordInFragment(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint = content.lastIndexOf(" ", start) != -1 ? content.lastIndexOf(" ", start) : start;
        int lastPoint = content.indexOf(" ", end + 100) != -1 ? content.indexOf(" ", end + 100) : content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }
}




