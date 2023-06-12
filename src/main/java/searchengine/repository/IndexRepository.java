package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Transactional
    void deleteALLByPageEntity(PageEntity pageEntity);
    Set<IndexEntity> findAllByLemmaEntityAndPageEntityIn(LemmaEntity lemmaEntity, Set<PageEntity> pageEntities);
    Set<IndexEntity> findAllByLemmaEntityId(int lemmaEntityId);
    Set <IndexEntity> findAllByPageEntityAndLemmaEntityIn(PageEntity pageEntity, Set<LemmaEntity> lemmaEntities);
    Set<IndexEntity> findAllByPageEntity(PageEntity pageEntity);
}
