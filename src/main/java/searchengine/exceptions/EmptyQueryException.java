package searchengine.exceptions;

public class EmptyQueryException extends RuntimeException {

    public EmptyQueryException(String message) {
        super(message);
    }
}
