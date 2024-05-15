package searchengine.dto.search;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
public class SearchData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
