package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


@Repository
@Transactional
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

    Optional<List<IndexEntity>> findByLemmaId(int lemmaId);

    List<IndexEntity> findByPageId(int pageId);

    Optional<IndexEntity> findByLemmaIdAndPageId(int lemmaId, int pageId);

}