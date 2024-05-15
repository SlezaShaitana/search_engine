package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    LemmaEntity findByLemmaIgnoreCaseAndSitesId(String lemma, Integer id);

    List<LemmaEntity> findBySitesId(Integer id);

    int countBySites_Id(Integer siteId);

    @Query(value = "SELECT l.* FROM lemma l WHERE l.lemma IN :lemmas AND l.site_id = :siteId", nativeQuery = true)
    List<LemmaEntity> findLemmasBySite(@Param("lemmas") List<String> lemmas, @Param("siteId") Integer siteId);
}
