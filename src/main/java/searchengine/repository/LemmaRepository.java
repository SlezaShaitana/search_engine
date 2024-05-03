package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
}
