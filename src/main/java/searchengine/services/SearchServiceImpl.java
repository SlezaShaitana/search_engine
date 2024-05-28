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

    @Override
    public ResponseEntity<Object> search(String query, String url, int offset, int limit) {
        if (query.isEmpty()) {
            throw new EmptyQueryException("Задан пустой поисковый запрос");
        } else {
            List<SearchData> searchData;
            if (!url.isEmpty()) {
                if (siteRepository.findByUrl(url) == null) {
                    throw new SiteUrlNotAllowedException("Указанная страница не найдена");

                } else {
                    searchData = onePageSearch(query, url, offset);
                }
            } else {
                searchData = searchThroughAllSites(query, offset);
            }
            if (searchData == null) {
                throw new SearchDataNotFoundException("NOT_FOUND");
            }

            List<SearchData> searchDataSublist = searchData.subList(offset, Math.min(offset + limit, searchData.size()));
            return new ResponseEntity<>(new SearchResponse(true, searchData.size(), searchDataSublist), HttpStatus.OK);
        }
    }

    @Override
    public List<SearchData> searchThroughAllSites(String query, int offset) {
        List<SiteEntity> sites = siteRepository.findAll();
        List<LemmaEntity> sortedLemmasPerSite = new ArrayList<>();
        List<String> lemmasFromQuery = getQueryIntoLemma(query);

        for (SiteEntity siteEntity : sites) {
            sortedLemmasPerSite.addAll(getLemmasFromSite(lemmasFromQuery, siteEntity));
        }
        List<SearchData> searchData = null;
        for (LemmaEntity lemmaEntity : sortedLemmasPerSite) {
            if (lemmasFromQuery.contains(lemmaEntity.getLemma())) {
                searchData = new ArrayList<>(getSearchDataList(sortedLemmasPerSite, lemmasFromQuery, offset));
                searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
            }
        }
        return searchData;
    }

    @Override
    public List<SearchData> onePageSearch(String query, String url, int offset) {
        SiteEntity siteEntity = siteRepository.findByUrl(url);
        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        List<LemmaEntity> lemmasFromSite = getLemmasFromSite(lemmasFromQuery, siteEntity);
        return getSearchDataList(lemmasFromSite, lemmasFromQuery, offset);
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

    private List<LemmaEntity> getLemmasFromSite(List<String> lemmas, SiteEntity site) {
        log.info("Get lemmas from site: {}", site.getUrl());
        ArrayList<LemmaEntity> lemmaList = (ArrayList<LemmaEntity>) lemmaRepository.findLemmasBySite(lemmas, site.getId());
        lemmaList.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return lemmaList;
    }


    private List<SearchData> getSearchDataList(List<LemmaEntity> lemmas, List<String> lemmasFromQuery,
                                               int offset) {
        List<SearchData> searchDataList = new ArrayList<>();
        List<Integer> lemmaIds = lemmas.stream().map(LemmaEntity::getId).toList();
        if (lemmas.size() >= lemmasFromQuery.size()) {

            System.out.println(lemmasFromQuery); // D E L E T E


            List<PageEntity> sortedPageList = pageRepository.findByLemmas(lemmaIds);
            List<Integer> pageIds = sortedPageList.stream().map(PageEntity::getId).toList();
            List<IndexEntity> sortedIndexList = indexRepository.findByLemmasAndPages(lemmaIds, pageIds);
            LinkedHashMap<PageEntity, Float> sortedPagesByAbsRelevance =
                    getSortPagesWithAbsRelevance(sortedPageList, sortedIndexList);
            List<SearchData> interimDataList = getSearchData(sortedPagesByAbsRelevance, lemmasFromQuery);

            if (offset > interimDataList.size()) {
                return new ArrayList<>();
            }
            for (int i = offset; i < interimDataList.size(); i++) {
                searchDataList.add(interimDataList.get(i));
            }
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
            String uri = pageEntity.getPath();
            String content = pageEntity.getContent();
            String title = connectToPage.getTitleFromHtml(content);
            SiteEntity siteEntity = pageEntity.getSites();
            String site = siteEntity.getUrl();
            String siteName = siteEntity.getName();
            Float absRelevance = sortedPages.get(pageEntity);
            String clearContent = lemmaFinder.removeHtmlTags(content);
            String snippet = getSnippet(clearContent, lemmasFromQuey);
            searchData.add(new SearchData(site, siteName, uri, title, snippet, absRelevance));
        }
        return searchData;
    }

    private String getSnippet(String content, List<String> lemmasFromQuey) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmasFromQuey) {
            lemmaIndex.addAll(lemmaFinder.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = extractAndHighlightWordsByLemmaIndex(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> extractAndHighlightWordsByLemmaIndex(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int step = i + 1;
            while (step < lemmaIndex.size() && lemmaIndex.get(step) - end > 0 && lemmaIndex.get(step) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(step));
                step += 1;
            }
            i = step - 1;
            String text = getWordsFromIndexWithHighlighting(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndexWithHighlighting(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 50) != -1) {
            lastPoint = content.indexOf(" ", end + 50);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }
}




