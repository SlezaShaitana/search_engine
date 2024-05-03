package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity sites;
    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @OneToMany(mappedBy = "lemmaId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IndexEntity> indexEntityList;

}
