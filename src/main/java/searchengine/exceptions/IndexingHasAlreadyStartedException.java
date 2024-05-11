package searchengine.exceptions;

public class IndexingHasAlreadyStartedException extends RuntimeException {
    public IndexingHasAlreadyStartedException(String message) {
        super(message);
    }
}
