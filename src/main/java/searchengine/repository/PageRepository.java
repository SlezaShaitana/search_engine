package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsByPath(String path);

    PageEntity findByPath(String path);

    int countBySites_id(Integer siteId);

    List<PageEntity> findBySites_Id(Integer siteId);

    @Query(value = "SELECT page.id, page.site_id, path, code, content " +
            "FROM page " +
            "WHERE page.id IN (" +
            "  SELECT i.page_id " +
            "  FROM `index` i " +
            "  JOIN lemma l on l.id = i.lemma_id " +
            "  WHERE l.id IN :lemmas " +
            "  GROUP BY i.page_id " +
            "  HAVING COUNT(DISTINCT i.lemma_id) = :lemmaCount)", nativeQuery = true)
    List<PageEntity> findByLemmas(@Param("lemmas") List<Integer> lemmas, @Param("lemmaCount") int lemmaCount);



}


