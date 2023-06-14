package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse stopIndexing() {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse indexPage(@RequestParam(name = "url") String url) throws Exception {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchResponse search(@RequestParam String query,
                                 @RequestParam(required = false) String site,
                                 @RequestParam(defaultValue = "0") int offset,
                                 @RequestParam(defaultValue = "20") int limit) throws IOException {
        return searchService.search(query, site, offset, limit);
    }
}
