package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class IndexDto {
    private Integer id;
    private PageEntity pages;
    private LemmaEntity lemma;
    private float rank;
}
