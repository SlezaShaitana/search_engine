package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private IndexationStatuses status;
    @LastModifiedDate
    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;


    @OneToMany(mappedBy = "sites", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PageEntity> pages;

    @OneToMany(mappedBy = "sites", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<LemmaEntity> lemmas;

//    @PrePersist
//    protected void onCreate() {
//        statusTime = LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        if (status == IndexationStatuses.INDEXING) {
//            statusTime = LocalDateTime.now();
//        }
//    }
}
