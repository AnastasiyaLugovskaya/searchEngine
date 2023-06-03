package searchengine.dto.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.JsoupConfiguration;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.HTMLParser;
import searchengine.services.LemmaParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.IndexingServiceImp.isIndexingStopped;

@RequiredArgsConstructor

public class Parser extends RecursiveAction {
    private final int siteId;
    private final String url;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private final HTMLParser htmlParser;
    private final JsoupConfiguration jsoupConfig;
    @Getter
    private static final ConcurrentHashMap<Integer, String> lastErrors = new ConcurrentHashMap<>();


    @Override
    protected void compute() {
        if (isIndexingStopped){
            return;
        }
        List<Parser> subTask = new ArrayList<>();
        Set<String> pageSet;
        try {
            SiteEntity site = siteRepository.findById(siteId);
            pageSet = htmlParser.getURLs(site.getUrl() + url);
            for (String path : pageSet) {
                if (isNotVisited(siteId, path) && isNotFailed(siteId)) {
                    boolean isSaved = savePage(site, path);
                    PageEntity pageEntity = pageRepository.findBySiteEntityIdAndPath(siteId, path);
                    LemmaParser lemmaParser = new LemmaParser(
                            siteRepository, pageRepository, lemmaRepository, indexRepository, site);
                    lemmaParser.parseOnePage(pageEntity);
                    updateSiteInfo(site, Status.INDEXING, lastErrors.get(siteId));
                    if (isSaved) {
                        Parser parser =
                                new Parser(siteId, path, siteRepository, pageRepository, lemmaRepository, indexRepository,
                                        htmlParser, jsoupConfig);
                        subTask.add(parser);
                    }
                }
            }
            invokeAll(subTask);
        } catch (Exception e) {
            e.printStackTrace();
            updateSiteInfo(siteRepository.findById(siteId), Status.FAILED, lastErrors.get(siteId));
        }
    }
    private boolean isNotVisited(int siteId, String path) {
        return !pageRepository.existsBySiteEntityIdAndPath(siteId, path);
    }

    private boolean isNotFailed(int siteId) {
        return !siteRepository.existsByIdAndStatus(siteId, Status.FAILED);
    }

    public void updateSiteInfo(SiteEntity site, Status status, String lastError){
        site.setStatusTime(System.currentTimeMillis());
        site.setStatus(status);
        if (lastError == null || lastError.length() == 0) {
            siteRepository.saveAndFlush(site);
        } else {
            site.setLastError(lastError);
            siteRepository.save(site);
        }
    }

    public boolean savePage(SiteEntity site, String path) throws IOException, InterruptedException {
        if (isIndexingStopped){
            return false;
        }
        Connection.Response response = htmlParser.getResponse(site.getUrl());
        String pageContent = htmlParser.getContent(response);
        int statusCode = htmlParser.getStatusCode(response);
        synchronized (pageRepository) {
            PageEntity page = pageRepository.findBySiteEntityIdAndPath(siteId, path);
            try {
                if (page == null) {
                    page = new PageEntity();
                    page.setPath(path);
                    page.setCode(statusCode);
                    page.setContent(pageContent);
                    page.setSiteEntity(site);
                    pageRepository.save(page);
                } else if (!page.getContent().equals(pageContent)) {
                    page.setContent(pageContent);
                    page.setCode(statusCode);
                    pageRepository.saveAndUpdate(page);
                }
            }catch (Exception e){
                String error = "Ошибка сохранения страницы - [" + path + "] -" +  System.lineSeparator() + e.getMessage();
                lastErrors.put(site.getId(), error);
            }
        }
        return true;
    }
}
