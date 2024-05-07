package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.IndexingHasAlreadyStartedException;
import searchengine.exceptions.IndexingIsNotRunningExceptions;
import searchengine.exceptions.SiteUrlNotAllowedException;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import searchengine.services.WebsiteIndexingServiceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final WebsiteIndexingServiceImpl indexingService;
    Map<String, Boolean> successfulResponse = new HashMap<>();
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, WebsiteIndexingServiceImpl indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
        successfulResponse.put("result", true);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "")
                                         String query,
                                         @RequestParam(name = "site", required = false, defaultValue = "")
                                         String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0")
                                         int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20")
                                         int limit) {

        return searchService.search(query, site, offset, limit);
    }


    @Async
    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (indexingService.isIndexing()) {
            throw new IndexingHasAlreadyStartedException("Индексация уже запущена");
        }
            new Thread(() -> indexingService.startIndexing()).start();
            return ResponseEntity.ok().body(successfulResponse);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stop() {
        if (!indexingService.isIndexing()) {
            throw new IndexingIsNotRunningExceptions("Индексация не запущена.");
        }
        indexingService.stopIndexing();
        return ResponseEntity.ok().body(successfulResponse);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> addOrUpdate(@RequestParam("url") String url) {
        if (!indexingService.existenceSiteInConfigurationFile(url)) {
            throw new SiteUrlNotAllowedException(
                    "Данная страница находится за пределами сайтов, " +
                            "указанных в конфигурационном файле");
        }
                    new Thread(() -> {
                        indexingService.addOrUpdate(url);
                    }).start();
            return ResponseEntity.accepted().body(successfulResponse);
    }
}
