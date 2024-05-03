package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.IndexationStatuses;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
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
    private List<String> pagesToCrawl;
    private final AtomicBoolean stopIndexingFlag;
    private ForkJoinPool pool;

    public IndexingRecursiveAction(String url, SiteEntity siteEntity, int maxDepth,
                                   int currentDepth, PageRepository pageRepository,
                                   SiteRepository siteRepository, AtomicBoolean stopIndexingFlag,
                                   ForkJoinPool pool) {
        this.url = url;
        this.siteEntity = siteEntity;
        this.maxDepth = maxDepth;
        this.currentDepth = currentDepth;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.stopIndexingFlag = stopIndexingFlag;
        this.pool = pool;
        pagesToCrawl = new ArrayList<>();
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
                if (!stopIndexingFlag.get()) {
                    siteEntity.setStatus(IndexationStatuses.INDEXING);
                    siteRepository.save(siteEntity);
                }
                Thread.sleep(400);
                log.info("Crawling page: {}", url);
                Connection connection = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .timeout(10000)
                        .ignoreHttpErrors(false)
                        .ignoreContentType(true)
                        .followRedirects(true)
                        .referrer("http://www.google.com");

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
                            pageRepository, siteRepository, stopIndexingFlag, pool);
                    subTasks.add(subTask);
                    subTask.fork();
                }

                for (IndexingRecursiveAction subtask : subTasks) {
                    subtask.join();
                }

                if (!stopIndexingFlag.get()) {
                    siteEntity.setStatus(IndexationStatuses.INDEXED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteEntity);
                }
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

    private void savingChildren(String childUrl, int statusCode, String content) {
        log.info("Received link: {}", childUrl);
        if (isCorrectUrl(childUrl)) {
            log.info("Link is valid");
            childUrl = stripParams(childUrl);
            synchronized (pageRepository) {
                boolean exists = pageRepository.existsByPath(childUrl);
                if (!exists && currentDepth < maxDepth) {
                    log.info("Page not found in list");
                    PageEntity pageEntity = new PageEntity();
                    pageEntity.setSites(siteEntity);
                    pageEntity.setPath(childUrl);
                    pageEntity.setContent(content);
                    pageEntity.setCode(statusCode);
                    log.info("Save page: {}", childUrl);
                    pageRepository.save(pageEntity);
                    pagesToCrawl.add(childUrl);
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