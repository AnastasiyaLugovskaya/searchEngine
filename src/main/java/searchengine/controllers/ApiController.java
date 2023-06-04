package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }
    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IndexingResponse startIndexing(){
        return indexingService.startIndexing();
    }
    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse stopIndexing(){
        return indexingService.stopIndexing();
    }
    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IndexingResponse indexPage(@RequestParam(name="url") String url) throws Exception {
        return indexingService.indexPage(url);
    }
}
