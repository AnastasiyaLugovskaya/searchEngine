package searchengine.services.lemma;

import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.*;

public class LemmaParser {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public LemmaParser(LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Transactional
    public void parseOnePage(PageEntity pageEntity) {
        try {
            LemmaService lemmaService = LemmaService.getInstance();

            Set<String> lemmaSet = lemmaService.getLemmaSet(pageEntity.getContent());
            Map<String, Integer> lemmasFromPage = lemmaService.getLemmas(pageEntity.getContent());

            Set<IndexEntity> indexEntitySet = new HashSet<>();
            for (String setStr : lemmaSet) {
                if (lemmasFromPage.get(setStr) != null) {
                    synchronized (lemmaRepository) {
                        Optional<LemmaEntity> optLemma = Optional.ofNullable(
                                lemmaRepository.findByLemmaAndSiteEntityId(setStr, pageEntity.getSiteEntity().getId()));
                        LemmaEntity lemma = new LemmaEntity();
                        lemma.setLemma(setStr);
                        lemma.setSiteEntity(pageEntity.getSiteEntity());
                        lemma.setFrequency(1);
                        if (optLemma.isPresent()) {
                            optLemma.get().setFrequency(optLemma.get().getFrequency() + 1);
                            lemma = lemmaRepository.save(optLemma.get());
                        } else {
                            lemma = lemmaRepository.save(lemma);
                        }
                        IndexEntity indexEntity = getIndexForLemma(lemma, pageEntity, lemmasFromPage);
                        indexEntitySet.add(indexEntity);
                    }
                }
            }
            indexRepository.saveAll(indexEntitySet);
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException("Ошибка при парсинге страницы");
        }
    }
    private IndexEntity getIndexForLemma(LemmaEntity lemma, PageEntity pageEntity, Map<String, Integer> lemmasFromPage){
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setLemmaEntity(lemma);
        indexEntity.setPageEntity(pageEntity);
        indexEntity.setRank(lemmasFromPage.get(lemma.getLemma()));
        return indexEntity;
    }
}
