package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsByPath(String path);

    PageEntity findByPath(String path);

    int countBySites_id(Integer siteId);

    List<PageEntity> findBySites_Id(Integer siteId);
}

