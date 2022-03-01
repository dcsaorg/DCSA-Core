package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
class ConflictException extends ConcreteRequestErrorMessageException {

    ConflictException(String reason, Object reference, String message, Throwable cause) {
        super(reason, reference, message, cause);
    }
}
