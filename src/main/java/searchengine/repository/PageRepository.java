package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
//    Optional<PageEntity> findPageEntityByPath(String path);
//Optional<PageEntity> findByPath(String path);

    boolean existsByPath(String path);

    //ЕСЛИ МЕТОДЫ НИЖЕ НЕ ПОНАДОБЯТСЯ УДАЛИТЬ


    @Query("SELECT p.path FROM PageEntity p")
    List<String> findAllPaths();
Optional<PageEntity> findAllByPath(String path);

    List<PageEntity> findByPath(String childUrl);
}

