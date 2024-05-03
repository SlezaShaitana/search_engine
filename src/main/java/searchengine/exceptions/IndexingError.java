package searchengine.exceptions;

import lombok.Data;

@Data
public class IndexingError {
    private boolean result;
    private String error;

    public IndexingError(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
