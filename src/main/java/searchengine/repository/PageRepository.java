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


    @Query(value = "SELECT p.* FROM page p JOIN `index` i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas",
            nativeQuery = true)
    List<PageEntity> findByLemmas(@Param("lemmas") List<Integer> lemmas);
}


