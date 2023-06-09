package searchengine.services;

import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LemmaParser {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public LemmaParser(LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }
    @Transactional
    public void parseOnePage(PageEntity pageEntity){
        try {
            LemmaService lemmaService = LemmaService.getInstance();

            Map<String, Integer> lemmasFromPage = lemmaService.getLemmas(pageEntity.getContent());
            Map<LemmaEntity, Integer> lemmaEntityMap = new HashMap<>();
            Set<IndexEntity> indexEntityList = new HashSet<>();
            synchronized (lemmaRepository) {
                lemmasFromPage.forEach((key, value) -> lemmaEntityMap.put(
                        updateLemmaInfo(key, pageEntity.getSiteEntity()), value));
                if (!lemmaEntityMap.isEmpty()) {
                    lemmaRepository.flush();
                    lemmaRepository.saveAll(lemmaEntityMap.keySet());
                }
            }
            lemmaEntityMap.forEach((key, value) -> {
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemmaEntity(key);
                indexEntity.setPageEntity(pageEntity);
                indexEntity.setRank(value);
                indexEntityList.add(indexEntity);
            });
            indexRepository.flush();
            indexRepository.saveAll(indexEntityList);
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException("Ошибка при парсинге страницы");
        }
    }
    public LemmaEntity updateLemmaInfo(String lemma, SiteEntity siteEntity){
        LemmaEntity lemmaEntity = lemmaRepository.findByLemmaAndSiteEntityId(lemma, siteEntity.getId());
        if (lemmaEntity == null){
            lemmaEntity = new LemmaEntity();
            lemmaEntity.setLemma(lemma);
            lemmaEntity.setSiteEntity(siteEntity);
            lemmaEntity.setFrequency(1);
        } else {
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
        }
        return lemmaEntity;
    }
}
