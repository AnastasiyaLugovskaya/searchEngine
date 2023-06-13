package searchengine.services.util;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfiguration;

import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class HTMLParser {
    private final JsoupConfiguration jsoupConfig;
    private static final Random random = new Random();

    public Set<String> getURLs(String content) throws InterruptedException {
        Set<String> urlSet = new TreeSet<>();
        Thread.sleep(500);
        Document doc = Jsoup.parse(content);

        Elements urls = doc.select("a[href]");
        urls.forEach(e -> {
            String link = e.attr("href");
            if (link.startsWith("/") && !link.contains("#") && !link.contains(".pdf")) {
                urlSet.add(link);
            }
        });
        return urlSet;
    }
    public Connection.Response getResponse(String url) throws InterruptedException, IOException {
        Thread.sleep(jsoupConfig.getTimeoutMin() + Math.abs(random.nextInt()) %
                jsoupConfig.getTimeoutMax() - jsoupConfig.getTimeoutMin());
        return Jsoup.connect(url)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .header("Accept-Language", "ru")
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .execute();
    }
    public String getContent(Connection.Response response){
        return response.body();
    }
    public int getStatusCode(Connection.Response response){
        return response.statusCode();
    }
    public String getTitle(String content) {
        Document document = Jsoup.parse(content);
        return document.title();
    }
}

