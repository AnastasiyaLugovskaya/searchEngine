package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfiguration;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.Parser;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

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
        cleanDB();
        addSitesToRepo(sites);
        for (SiteEntity siteEntity : siteRepository.findAll()) {
            indexOneSite(siteEntity, "/", siteRepository, pageRepository, htmlParser, jsoupConfig);
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

    @Override
    public IndexingResponse indexPage(String url) throws Exception {
        IndexingResponse response = new IndexingResponse();
        url = url.toLowerCase();
        Site siteToMatch = null;
        for (Site site : sites.getSites()){
            if (url.contains(site.getUrl())){
                siteToMatch = site;
            }
        }
        if (siteToMatch == null){
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
            return response;
        }

        String root = siteToMatch.getUrl();
        String relativeUrl = (url.equals(root)) ? "/" : url.replace(root, "");
        SiteEntity siteEntity = siteRepository.findByUrl(root);
        if (siteEntity == null){
            addOneSiteToRepo(siteToMatch);
            siteEntity = siteRepository.findByUrl(root);
        }
        deletePage(siteEntity.getId(), relativeUrl);
        indexOneSite(siteEntity, relativeUrl, siteRepository, pageRepository, htmlParser, jsoupConfig);

        response.setResult(true);
        response.setError("");
        return response;
    }
    public void cleanDB(){
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }
    private void indexOneSite(SiteEntity siteEntity, String url, SiteRepository siteRepository, PageRepository pageRepository,
                                HTMLParser htmlParser, JsoupConfiguration jsoupConfig) {
        new Thread(() -> {
                    Parser pageParser = new Parser(
                            siteEntity.getId(), url, siteRepository, pageRepository, htmlParser, jsoupConfig);
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
    }
    private void addSitesToRepo(SitesList sites){
        for (Site s : sites.getSites()) {
            addOneSiteToRepo(s);
        }
    }
    private void addOneSiteToRepo(Site s){
        SiteEntity entity = new SiteEntity();
        entity.setName(s.getName());
        entity.setUrl(s.getUrl().toLowerCase());
        entity.setStatus(Status.INDEXING);
        entity.setStatusTime(System.currentTimeMillis());

        siteRepository.save(entity);
    }
    private void deletePage(int siteEntityId, String path) throws Exception {
        PageEntity page = pageRepository.findBySiteEntityIdAndPath(siteEntityId, path);
        if (page != null){
            pageRepository.delete(page);
        }
    }
}
