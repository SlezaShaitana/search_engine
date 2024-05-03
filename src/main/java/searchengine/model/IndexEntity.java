package searchengine.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "`index`") //заэкранировали иначе таблица не создавалась
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
//    @EmbeddedId
    private Integer id;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "page_id", nullable = false)
    @Column(name = "page_id", nullable = false, insertable = false, updatable = false)
    private int pageId;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "lemma_id", nullable = false)
    @Column(name = "lemma_id", nullable = false, insertable = false, updatable = false)
    private int lemmaId;
    @Column(name = "`rank`", columnDefinition = "FLOAT", nullable = false)
    private Float rank;


}
