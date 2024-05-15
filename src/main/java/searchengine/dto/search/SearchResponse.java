package searchengine.dto.search;

import lombok.*;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;
    List<SearchData> data;
}
