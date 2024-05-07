package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.SiteEntity;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class LemmaDto {
    private Integer id;
    private SiteEntity sites;
    private String lemma;
    private Integer frequency;
}
