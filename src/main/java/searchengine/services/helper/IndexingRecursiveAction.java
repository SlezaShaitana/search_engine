package searchengine.services.helper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Slf4j
public class IndexingRecursiveAction extends RecursiveAction {
    private final String url;
    private final SiteEntity siteEntity;
    private final int maxDepth;
    private int currentDepth;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private ConcurrentLinkedQueue<String> pagesToCrawl;
    private final AtomicBoolean stopIndexingFlag;
    private ForkJoinPool pool;
    private LemmaFinder lemmaFinder = new LemmaFinder();
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final EntityFactory entityFactory;
    private final ConnectToPage connectToPage;

    public IndexingRecursiveAction(String url, SiteEntity siteEntity, int maxDepth,
                                   int currentDepth, PageRepository pageRepository,
                                   SiteRepository siteRepository, AtomicBoolean stopIndexingFlag,
                                   ForkJoinPool pool, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.url = url;
        this.siteEntity = siteEntity;
        this.maxDepth = maxDepth;
        this.currentDepth = currentDepth;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.stopIndexingFlag = stopIndexingFlag;
        this.pool = pool;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        pagesToCrawl = new ConcurrentLinkedQueue<>();
        entityFactory = new EntityFactory(lemmaRepository, pageRepository, indexRepository);
        connectToPage = new ConnectToPage();
    }

    @Override
    protected void compute() {
        if (pool.isShutdown() || stopIndexingFlag.get() || currentDepth > maxDepth) {
            return;
        }
        compute(currentDepth);
    }

    private void compute(int currentDepth) {
        try {
            Thread.sleep(400);
            settingSiteStatus(IndexationStatuses.INDEXING);
            log.info("Crawling page: {}", url);
            Connection connection = connectToPage.connectToPage(url);
            Document page = connection.get();
            String content = page.toString();
            int statusCode = connection.response().statusCode();
            Elements elements = page.select("a[href], link[href]");
            for (Element e : elements) {
                String childUrl = e.attr("abs:href");
                savingChildren(childUrl, statusCode, content);
            }
            List<IndexingRecursiveAction> subTasks = new ArrayList<>();
            for (String childUrl : pagesToCrawl) {
                IndexingRecursiveAction subTask = new IndexingRecursiveAction(url +
                        childUrl, siteEntity, maxDepth, currentDepth + 1,
                        pageRepository, siteRepository, stopIndexingFlag, pool, lemmaRepository, indexRepository);
                subTasks.add(subTask);
                subTask.fork();
            }
            for (IndexingRecursiveAction subtask : subTasks) {
                subtask.join();
            }
            settingSiteStatus(IndexationStatuses.INDEXED);
        } catch (HttpStatusException e) {
            log.error(e.getMessage());
            if (siteEntity.getUrl().equals(url)) {
                siteEntity.setLastError("Could not connect to site: " + url +
                        " .Error message: " + e);
                siteRepository.save(siteEntity);
            } else {
                int statusCode = e.getStatusCode();
                savingChildren(url, statusCode, "Failed to load page content. Error:" + e.getMessage());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void settingSiteStatus(IndexationStatuses status) {
        if (!stopIndexingFlag.get()) {
            siteEntity.setStatus(status);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
        }
    }

    private void savingChildren(String childUrl, int statusCode, String content) {
        if (stopIndexingFlag.get()) {
            return;
        }
        log.info("Received link: {}", childUrl);
        if (isCorrectUrl(childUrl)) {
            log.info("Link is valid");
            childUrl = stripParams(childUrl);
            synchronized (pageRepository) {
                boolean exists = pageRepository.existsByPath(childUrl);
                if (!exists && currentDepth < maxDepth) {
                    log.info("Page not found in list");
                    PageEntity pageEntity = entityFactory.createPageEntity(siteEntity, childUrl, content, statusCode);
                    pagesToCrawl.add(childUrl);
                    HashMap<String, Integer> lemmasList = lemmaFinder.collectLemmas(content);
                    for (Map.Entry<String, Integer> lemma : lemmasList.entrySet()) {
                        float count = lemma.getValue();
                        LemmaEntity lemmaEntity = lemmaRepository.findByLemmaIgnoreCaseAndSitesId(lemma.getKey(), siteEntity.getId());
                        IndexEntity indexEntityUniquePage = indexRepository.findByLemmaIdAndPageId(lemmaEntity, pageEntity);
                        if (lemmaEntity != null) {
                            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                            lemmaRepository.save(lemmaEntity);
                            if (indexEntityUniquePage != null) {
                                indexEntityUniquePage.setRank(count);
                                indexRepository.save(indexEntityUniquePage);
                            }
                            if (indexEntityUniquePage == null) {
                                entityFactory.createIndexEntity(pageEntity, lemmaEntity, count);
                            }
                        } else {
                            LemmaEntity newLemma = entityFactory.createLemmaEntity(siteEntity, lemma.getKey());
                            entityFactory.createIndexEntity(pageEntity, newLemma, count);
                        }
                    }
                }
            }
        }
    }

    private String stripParams(String urlForCorrection) {
        String urlStripParams = siteEntity.getUrl().replaceAll("https://(www.)?", "");
        log.info("Trimming root site");
        return urlForCorrection.replaceAll("https://" + urlStripParams, "")
                .replaceAll(siteEntity.getUrl(), "");
    }

    private boolean isCorrectUrl(String urlChild) {
        log.info("Checking link validity");
        String urlStripParams = siteEntity.getUrl().replaceAll("https://(www.)?", "");
        Pattern patternRoot = Pattern.compile("^https?://(www\\.)?" + urlStripParams);
        Pattern patternNotFile = Pattern.compile("([^\\s]+(\\.(?i)(jpg|css|jpeg|webp|doc|" +
                "png|gif|bmp|pdf))(\\?[\\w\\-]+=\\w+)*)");
        Pattern patternNotAnchor = Pattern.compile("#([\\w\\-]+)?$");
        return patternRoot.matcher(urlChild).lookingAt()
                && !patternNotFile.matcher(urlChild).find()
                && !patternNotAnchor.matcher(urlChild).find();
    }
}