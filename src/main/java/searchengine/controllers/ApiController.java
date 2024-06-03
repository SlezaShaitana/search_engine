package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.ResponseDto;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.IndexingHasAlreadyStartedException;
import searchengine.exceptions.IndexingIsNotRunningExceptions;
import searchengine.exceptions.SiteUrlNotAllowedException;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import searchengine.services.WebsiteIndexingServiceImpl;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final WebsiteIndexingServiceImpl indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, WebsiteIndexingServiceImpl indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
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
        if (limit < 20) {
            limit = 20;
        }
        return searchService.searchByQueryAndUrlWithPagination(query, site, offset, limit);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (indexingService.isIndexing()) {
            throw new IndexingHasAlreadyStartedException("Индексация уже запущена");
        }
        indexingService.startIndexing();
        return ResponseEntity.ok().body(new ResponseDto(true));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stop() {
        if (!indexingService.isIndexing()) {
            throw new IndexingIsNotRunningExceptions("Индексация не запущена.");
        }
        indexingService.stopIndexing();
        return ResponseEntity.ok().body(new ResponseDto(true));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> addOrUpdate(@RequestParam("url") String url) {
        if (!indexingService.existenceSiteInConfigurationFile(url)) {
            throw new SiteUrlNotAllowedException(
                    "Данная страница находится за пределами сайтов, " +
                            "указанных в конфигурационном файле");
        }
        indexingService.addOrUpdate(url);
        return ResponseEntity.accepted().body(new ResponseDto(true));
    }
}
