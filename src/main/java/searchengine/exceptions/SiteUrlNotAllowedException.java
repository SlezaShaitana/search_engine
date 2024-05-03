package searchengine.exceptions;

public class SiteUrlNotAllowedException extends RuntimeException{

    public SiteUrlNotAllowedException(String message) {
        super(message);
    }
}
