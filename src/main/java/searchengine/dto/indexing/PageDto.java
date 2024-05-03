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
public class PageDto {
    private Integer id;
    private SiteEntity sites;
    private String path;
    private Integer code;
    private String content;

    private Integer siteId;
}
