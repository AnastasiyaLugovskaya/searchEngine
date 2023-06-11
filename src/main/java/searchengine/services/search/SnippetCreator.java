package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.services.lemma.LemmaService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class SnippetCreator {
    private static final int MAX_SNIPPET_LENGTH = 210;
    private LemmaService lemmaService;

    public String createSnippet(String content, Set<String> lemmas) throws IOException {
        lemmaService = LemmaService.getInstance();
        StringBuilder builder = new StringBuilder();
        String[] words = content.trim().split("\\s+");
        Set<String> foundWords = new TreeSet<>();
        for (int i = 0; i < words.length; i++) {
            String word = words[i].trim();
            String lemmaWord = lemmaService.getOneLemma(word);
            if (word.length() == 0 || lemmaWord == null) {
                continue;
            }
            if (lemmas.contains(lemmaWord)){
                foundWords.add(word.replaceAll("[^a-zA-Zа-яА-ЯёЁ]", ""));
                int start = Math.max(i - 5, 0);
                int end = Math.min(i + 5, words.length);
                int totalLength = builder.length();
                List<String> subList = List.of(words).subList(start, end);
                for (String subWord : subList) {
                    totalLength += subWord.length();
                }
                if (totalLength >= 5) {
                    end = i;
                }
                builder.append(start > 0 ? "..." : "");
                subList.forEach(w -> {
                    builder.append(w).append(" ");
                });
                if (builder.length() > MAX_SNIPPET_LENGTH) {
                    break;
                }
                builder.append(end < words.length ? "...\n" : "");
            }
        }
        String snippet = builder.toString();
        if (snippet.length() > 0){
            for (String word : foundWords) {
                snippet = snippet.replaceAll(word, "<b>" + word + "</b>");
            }
        }
        return snippet;
    }
}
