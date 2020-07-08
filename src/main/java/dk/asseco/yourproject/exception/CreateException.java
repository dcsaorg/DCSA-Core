package dk.asseco.yourproject.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CreateException extends RuntimeException {
    public CreateException(String errorMessage) {
        super(errorMessage);
    }
}
