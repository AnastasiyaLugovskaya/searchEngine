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
    private final static String REGEX_TO_REMOVE_TAGS = "(?i)<[^>]*>";
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
        Map<String, Integer> lemmas = lemmaService.getLemmas("<div class=\"fmain\">\n" +
                "\t\n" +
                "\t\t<div class=\"fcols fx-row\">\n" +
                "\t\t\n" +
                "\t\t\t<div class=\"fleft fx-1 fx-row\">\n" +
                "\t\t\t\n" +
                "\t\t\t\t<div class=\"fleft-desc fx-1\">\n" +
                "\t\t\t\t\t<div style=\"margin-bottom: 20px\" id=\"movie_video\"></div>\n" +
                "\t\t\t\t\t<h1>Извне <small>1,2 сезон смотреть онлайн</small></h1>\n" +
                "\t\t\t\t\t<div class=\"fdesc clearfix\">\n" +
                "\t\t\t\t\t\tСобытия разворачиваются в небольшом провинциальном городке, затерявшемся где-то посреди американских лесов. Безумно красивые виды вынуждают путешественников заглядывать сюда с целью немного отдохнуть от изнурительной дороги и наполниться положительными эмоциями. Путники не догадываются, что выбраться из поселения им будет чрезвычайно трудно. Всё население города состоит из таких гостей, вынужденных при свете дня налаживать быт, а под покровом ночи скрываться от страшных лесных монстров. Весьма неприятное соседство доставляет народу немало хлопот. Животный страх буквально сковывает горожан, а сложившаяся ловушка кажется абсолютно непреодолимой. Смотрите в HD 1080 качестве, как главным героям сериала «Извне» в обозримом будущем придётся узнать, кто же прячется в зарослях. Следует заметить, что коварные чудовища умеют гипнотизировать жертв, полностью лишая их способности атаковать или спасаться. Возможно, кому-то из смельчаков всё же удастся найти метод оказания достойного сопротивления. Только люди, получившие печальный опыт, могут определить новые правила игры. Даже в самом крепком заборе можно найти брешь, и персонажам необходимо сделать это в максимально сжатые сроки.\n" +
                "\t\t\t\t\t</div>");
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
        text = removeTagsFromText(text);
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
    private String removeTagsFromText(String content){
        return content.replaceAll(REGEX_TO_REMOVE_TAGS, " ").replaceAll("\\s+", " ").trim();
    }
}
