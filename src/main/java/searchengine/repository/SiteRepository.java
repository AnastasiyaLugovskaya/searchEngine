package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Component
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    boolean existsByIdAndStatus(int siteId, Status status);
    int countByStatus(Status status);
    SiteEntity findByUrl(String url);
    SiteEntity findById(int siteId);
    }
