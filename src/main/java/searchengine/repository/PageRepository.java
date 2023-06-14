package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    int countPagesBySiteEntity(SiteEntity siteEntity);

    boolean existsBySiteEntityIdAndPath(int siteEntityId, String path);

    PageEntity findBySiteEntityIdAndPath(int siteEntityId, String path);
}
