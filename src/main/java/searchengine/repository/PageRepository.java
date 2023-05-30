package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    PageEntity findById(int id);
    int countPagesBySiteEntity (SiteEntity siteEntity);
    boolean existsBySiteEntityIdAndPath(int siteEntityId, String path);
    PageEntity findBySiteEntityAndPath(SiteEntity siteEntity, String path);
    @Transactional
    @Modifying
    default int saveAndUpdate(PageEntity pageEntity) {
        Optional<PageEntity> entityOpt =
                Optional.ofNullable(findBySiteEntityAndPath(pageEntity.getSiteEntity(), pageEntity.getPath()));
        if (entityOpt.isPresent()) {
            PageEntity updatablePage = entityOpt.get();
            if (updatablePage.getCode() == 200) {
                pageEntity = updatablePage;
                return pageEntity.getId();
            }
            updatablePage.setCode(pageEntity.getCode());
            updatablePage.setContent(pageEntity.getContent());
            pageEntity = save(updatablePage);
        } else {
            pageEntity = save(pageEntity);
        }
        return pageEntity.getId();
    }

}
