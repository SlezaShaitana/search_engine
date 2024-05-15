package searchengine.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({IndexingHasAlreadyStartedException.class})
    protected ResponseEntity<IndexingError> handleException(IndexingHasAlreadyStartedException e) {
        log.error("Application specific error handling", e);
        return new ResponseEntity<>(new IndexingError(false, e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({SiteUrlNotAllowedException.class})
    protected ResponseEntity<IndexingError> handleException(SiteUrlNotAllowedException e) {
        log.error("Application specific error handling", e);
        return new ResponseEntity<>(new IndexingError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({EmptyQueryException.class})
    public ResponseEntity<IndexingError> handleException(EmptyQueryException e) {
        log.error("Application specific error handling", e);
        return new ResponseEntity<>(new IndexingError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({IndexingIsNotRunningExceptions.class})
    public ResponseEntity<IndexingError> handleException(IndexingIsNotRunningExceptions e) {
        log.error("Application specific error handling", e);
        return new ResponseEntity<>(new IndexingError(false, e.getMessage()), HttpStatus.NOT_MODIFIED);
    }

    @ExceptionHandler({SearchDataNotFoundException.class})
    public ResponseEntity<IndexingError> handleException(SearchDataNotFoundException e) {
        log.error("Application specific error handling", e);
        return new ResponseEntity<>(new IndexingError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
