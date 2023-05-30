package searchengine.dto.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import searchengine.config.JsoupConfiguration;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.HTMLParser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.IndexingServiceImp.isIndexingStopped;

@RequiredArgsConstructor

public class Parser extends RecursiveAction {
    private final String url;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    private final HTMLParser htmlParser;
    private final JsoupConfiguration jsoupConfig;
    private final SitesList sitesList;
//    @Getter
//    private volatile String lastError;
    @Getter
    private static final ConcurrentHashMap<Integer, String> lastErrors = new ConcurrentHashMap<>();


    @Override
    protected void compute() {
        if (isIndexingStopped){
            return;
        }
        List<Parser> subTask = new ArrayList<>();
        TreeSet<String> pageSet;
        try {
            pageSet = parse(url);
            SiteEntity site = siteRepository.findByUrl(url);
            if (site == null) {
                for (Site s : sitesList.getSites()) {
                    if (url.contains(s.getUrl())) {
                        site = siteRepository.findByUrl(s.getUrl());
                    }
                }
            }
            int siteId = site.getId();
            for (String path : pageSet) {
                if (isNotVisited(siteId, path) && isNotFailed(siteId)) {
                    boolean isSaved = savePage(site, path);
                    updateSiteInfo(site, Status.INDEXING, lastErrors.get(siteId));
                    if (isSaved) {
                        Parser parser =
                                new Parser(path, siteRepository, pageRepository, htmlParser, jsoupConfig, sitesList);
                        subTask.add(parser);
                    }
                }
            }
            invokeAll(subTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TreeSet<String> parse(String url) throws InterruptedException, IOException {
        TreeSet<String> urlSet = new TreeSet<>();
        Thread.sleep(150);
        Document doc = Jsoup.connect(url)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer()).get();
        Elements urls = doc.select("a[href]");
        urls.forEach(e -> {
            String link = e.attr("abs:href");
            if (link.startsWith(url) && !link.contains("#") && !link.contains(".pdf") && !link.equals(url)
                    && !link.endsWith("/")) {
                urlSet.add(link);
            }
        });
        return urlSet;
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

    private boolean savePage(SiteEntity site, String path) throws IOException, InterruptedException {
        if (isIndexingStopped){
            return false;
        }
        Connection.Response response = htmlParser.getResponse(site.getUrl());
        String pageContent = htmlParser.getContent(response);
        int statusCode = htmlParser.getStatusCode(response);
        synchronized (pageRepository) {
            PageEntity page = pageRepository.findBySiteEntityAndPath(site, path);
            try {
                if (page == null) {
                    page = new PageEntity();
                    page.setPath(Utils.getRelativeUrl(path, sitesList));
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

//    public boolean cancelTask(boolean mayInterruptIfRunning, List<Parser> tasks) {
//        boolean result = true;
//        for (RecursiveAction task : tasks) {
//            result = task.cancel(mayInterruptIfRunning) && result;
//        }
//        return super.cancel(mayInterruptIfRunning) && result;
//    }
}
