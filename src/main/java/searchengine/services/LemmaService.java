package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LemmaService {
    private final static String REGEX_TO_SPLIT = "[^а-яё\\s]";
    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private static LuceneMorphology luceneMorphology;

    public LemmaService(LuceneMorphology morphology) {
        luceneMorphology = morphology;
    }

    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);
        LemmaService lemmaService = new LemmaService(luceneMorph);
        Map<String, Integer> lemmas = lemmaService.getLemmas("Затерянный в лесу американский городок пленяет каждого, " +
                "кому не посчастливилось проехать его по пути — покинуть это место оказывается буквально невозможно. " +
                "Днем невольные жители пытаются выстроить подобие нормального быта и найти способ выбраться, " +
                "а по ночам спасаются от пугающих обитателей леса, которые приходят навестить горожан, " +
                "как только заходит солнце.");
        for (String key : lemmas.keySet()){
            Integer value = lemmas.get(key);
            System.out.println(key + " - " + value);
        }
    }
    public static LemmaService getInstance() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        return new LemmaService(morphology);
    }
    public Map<String, Integer> getLemmas(String text) throws IOException {
        Map<String, Integer> lemmas = new HashMap<>();
        if (text == null || text.length() == 0) {
            return lemmas;
        }
        String[] words = text.toLowerCase(Locale.ROOT)
                .replaceAll(REGEX_TO_SPLIT, " ")
                .trim()
                .split("\\s+");
        for (String word : words){
            if (word.isBlank()){
                continue;
            }
            word = word.trim();
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }
            String normalWord = normalForms.get(0).replaceAll("ё", "е");
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        return lemmas;
    }
    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}
