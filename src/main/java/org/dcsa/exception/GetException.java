package org.dcsa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class GetException extends RuntimeException {
    public GetException(String errorMessage) {
        super(errorMessage);
    }
}
