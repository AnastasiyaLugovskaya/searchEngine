package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import searchengine.config.JsoupConfiguration;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.util.HTMLParser;
import searchengine.services.lemma.LemmaParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.indexing.IndexingServiceImp.isIndexingStopped;

@RequiredArgsConstructor

public class Parser extends RecursiveAction {
    private final int siteId;
    private final String path;
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
            updateSiteInfo(site, Status.INDEXING, lastErrors.get(siteId));
            if (isNotVisited(siteId, path) || isNotFailed(siteId)) {
                Optional<PageEntity> optPage = savePage(site, path);
                if (optPage.isPresent()) {
                    PageEntity pageEntity = optPage.get();
                    LemmaParser lemmaParser = new LemmaParser(lemmaRepository, indexRepository);
                    if (HttpStatus.valueOf(pageEntity.getCode()).is2xxSuccessful()) {
                        lemmaParser.parseOnePage(pageEntity);
                    }
                    pageSet = htmlParser.getURLs(pageEntity.getContent());
                    for (String path : pageSet) {
                        Parser parser =
                                new Parser(siteId, path, siteRepository, pageRepository, lemmaRepository, indexRepository,
                                        htmlParser, jsoupConfig);
                        subTask.add(parser);
                    }
                    invokeAll(subTask);
                }
            }
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
    public Optional<PageEntity> savePage(SiteEntity site, String path) throws IOException, InterruptedException {
        if (isIndexingStopped){
            return Optional.empty();
        }
        Connection.Response response = htmlParser.getResponse(site.getUrl() + path);
        String pageContent = htmlParser.getContent(response);
        int statusCode = htmlParser.getStatusCode(response);

        pageRepository.flush();
        PageEntity page = new PageEntity();
        synchronized (pageRepository) {
            if (isNotVisited(site.getId(), path)) {
                try {
                    Optional<PageEntity> optPage = Optional.ofNullable(
                            pageRepository.findBySiteEntityIdAndPath(siteId, path));
                    if (optPage.isEmpty()) {
                        page.setPath(path);
                        page.setCode(statusCode);
                        page.setContent(pageContent);
                        page.setSiteEntity(site);
                        page = pageRepository.save(page);
                    } else if (!optPage.get().getContent().equals(pageContent)) {
                        page.setContent(pageContent);
                        page.setCode(statusCode);
                        page = pageRepository.save(page);
                    }
                } catch (Exception e) {
                    String error = "Ошибка сохранения страницы - [" + path + "] -" + System.lineSeparator() + e.getMessage();
                    lastErrors.put(site.getId(), error);
                }
                return Optional.of(page);
            } else {
                return Optional.empty();
            }
        }
    }
}
