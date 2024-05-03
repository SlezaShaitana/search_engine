package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexationStatuses;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer>, QueryByExampleExecutor<SiteEntity> {




//    Optional<SiteEntity> findByUrl(String url);
    SiteEntity findByUrl(String url);

    List<SiteEntity> getSiteEntityByUrl(String url);

    List<SiteEntity> findAllByStatus(IndexationStatuses status);




}
