package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfiguration;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.Parser;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor

public class IndexingServiceImp implements IndexingService {
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    private final JsoupConfiguration jsoupConfig;
    private final HTMLParser htmlParser;
    public static volatile boolean isIndexingStopped = false;

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        boolean isIndexingStarted = false;
        if (siteRepository.countByStatus(Status.INDEXING) > 0) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
        for (Site s : sites.getSites()) {
            SiteEntity entity = new SiteEntity();
            entity.setName(s.getName());
            entity.setUrl(s.getUrl().toLowerCase());
            entity.setStatus(Status.INDEXING);
            entity.setStatusTime(System.currentTimeMillis());

            siteRepository.save(entity);
        }
        for (SiteEntity siteEntity : siteRepository.findAll()) {
            new Thread(() -> {
                Parser pageParser = new Parser(
                        siteEntity.getUrl(), siteRepository, pageRepository, htmlParser, jsoupConfig, sites);
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                forkJoinPool.invoke(pageParser);
                if (isIndexingStopped) {
                    pageParser.cancel(true);
                    pageParser.updateSiteInfo(siteEntity, Status.FAILED, "Индексация остановлена пользователем");
                }
                if (siteEntity.getStatus() != Status.FAILED && !isIndexingStopped) {
                    pageParser.updateSiteInfo(siteEntity, Status.INDEXED, Parser.getLastErrors().get(siteEntity.getId()));
                }
            }).start();
            isIndexingStarted = true;
        }
        response.setResult(isIndexingStarted);
        response.setError("");
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (siteRepository.countByStatus(Status.INDEXING) == 0) {
            response.setResult(false);
            response.setError("Индексация не запущена");
        } else {
            isIndexingStopped = true;
            response.setResult(true);
            response.setError("");
        }
        return response;
    }

/*
    private void indexAllSites(SitesList sites) {
        for (Site site : sites.getSites()) {
            Thread thread = new Thread(() -> indexOneSite(site));
            thread.setName(site.getName());
            thread.start();
        }
    }

    private void indexOneSite(Site site) {

    }

    private boolean isIndexing(String name, Status status) {
        return siteRepository.existsByNameAndStatus(name, status);
    }
*/
}
