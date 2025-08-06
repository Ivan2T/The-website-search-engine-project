package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


@Repository
@Transactional
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    Optional<SiteEntity> findByName(String name);

    List<SiteEntity> findSiteEByName(String name);

    int countByNameAndStatus(String name, Status indexing);

    boolean existsByName(String name);

    SiteEntity getSiteEBySiteId(int siteId);
}