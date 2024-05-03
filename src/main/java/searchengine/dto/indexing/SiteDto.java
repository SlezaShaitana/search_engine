package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.IndexationStatuses;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SiteDto {
    private Integer id;
    private IndexationStatuses status;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private String name;
}
