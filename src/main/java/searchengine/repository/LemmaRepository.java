package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    LemmaEntity findByLemmaIgnoreCaseAndSitesId(String lemma, Integer id);

    List<LemmaEntity> findBySitesId(Integer id);

    int countBySites_Id(Integer siteId);
}
