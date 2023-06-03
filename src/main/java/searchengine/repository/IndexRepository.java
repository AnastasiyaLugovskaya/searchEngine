package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Transactional
    void deleteByPageEntity(PageEntity pageEntity);
}
