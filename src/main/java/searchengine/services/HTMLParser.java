package searchengine.services;

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

    public Set<String> getURLs(String url) throws InterruptedException, IOException {
        Set<String> urlSet = new TreeSet<>();
        Thread.sleep(500);

        Connection.Response response = getResponse(url);
        if (response.contentType() != null && ((response.contentType().contains("text/html")
                || response.contentType().contains("xml"))))  {
            Document doc = Jsoup.parse(getContent(response));

            Elements urls = doc.select("a[href]");
            urls.forEach(e -> {
                String link = e.attr("href");
                if (link.startsWith("/") && !link.contains("#") && !link.contains(".pdf")) {
                    urlSet.add(link);
                }
            });
        }
        return urlSet;
    }
    public Connection.Response getResponse(String url) throws InterruptedException, IOException {
        Thread.sleep(jsoupConfig.getTimeoutMin() + Math.abs(random.nextInt()) %
                jsoupConfig.getTimeoutMax() - jsoupConfig.getTimeoutMin());
        Connection.Response response = Jsoup.connect(url)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .header("Accept-Language", "ru")
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .execute();
        return response;
    }
    public String getContent(Connection.Response response) throws IOException {
        return response.parse().html();
    }
    public int getStatusCode(Connection.Response response) throws IOException {
        return response.statusCode();
    }
    public String getTitle(String content) {
        Document document = Jsoup.parse(content);
        return document.title();
    }
}

