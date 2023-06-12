package searchengine.services.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.lemma.LemmaService;

import java.io.IOException;
import java.util.*;

public class SnippetMaker {
    private LemmaService lemmaService = LemmaService.getInstance();
    private List<String> queryLemmas = new ArrayList<>();
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;
    PageEntity page;

    public SnippetMaker() throws IOException {
    }
    public String getSnippet(String content, Set<String> lemmas, SiteEntity siteEntity){
        return null;
    }

    public String getAt(String st, int pos) throws IOException {
        StringBuilder sb = new StringBuilder();
        String[] tokens = st.split(" ");

        int pre = 10;
        int post = 10;
        if (pos < pre) {
            pre = pos;
            post = post + 10 - pos;
        }
        if (pos > tokens.length - post) {
            post = tokens.length - post - 1;
            pre = pre + (10 - post);
        }
        for (int i = pos - pre; i < pos + post; i++) {
            if (lemmaService.getLemmas(tokens[i]).size() > 0 &&
                    queryLemmas.contains(lemmaService.getLemmas(tokens[i]).keySet().stream().findFirst().orElse(""))) {
                sb.append("<b>").append(tokens[i]).append("</b>");
            } else {
                sb.append(tokens[i]);
            }
            sb.append(" ");
        }
        return sb.toString();
    }
}
