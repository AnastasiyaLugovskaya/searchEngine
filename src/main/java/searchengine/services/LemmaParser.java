package searchengine.services;

import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmaParser {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private SiteEntity siteEntity;

    public LemmaParser(SiteRepository siteRepository, PageRepository pageRepository,
                       LemmaRepository lemmaRepository, IndexRepository indexRepository,
                       SiteEntity siteEntity) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteEntity = siteEntity;
    }
    @Transactional
    public void parseOnePage(PageEntity pageEntity){
        try {
            LemmaService lemmaService = LemmaService.getInstance();

            Map<String, Integer> lemmasFromPage = lemmaService.getLemmas(pageEntity.getContent());
            Map<LemmaEntity, Integer> lemmaEntityMap = new HashMap<>();
            List<IndexEntity> indexEntityList = new ArrayList<>();

            lemmasFromPage.forEach((key, value) -> lemmaEntityMap.put(updateLemmaInfo(key), value));
            lemmaRepository.saveAll(lemmaEntityMap.keySet());

            lemmaEntityMap.forEach((key, value) -> {
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemmaEntity(key);
                indexEntity.setPageEntity(pageEntity);
                indexEntity.setRank(value);
                indexEntityList.add(indexEntity);
            });
            indexRepository.saveAll(indexEntityList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public LemmaEntity updateLemmaInfo(String lemma){
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

    public void setSiteEntity(SiteEntity siteEntity) {
        this.siteEntity = siteEntity;
    }
}
