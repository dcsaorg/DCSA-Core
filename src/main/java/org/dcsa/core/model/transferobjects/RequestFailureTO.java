package org.dcsa.core.model.transferobjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RequestFailureTO {

    private final String httpMethod;

    private final String requestUri;

    private final List<ConcreteRequestErrorMessageTO> errors;

    @JsonIgnore
    private final HttpStatus httpStatus;

    public int getStatusCode() {
        return httpStatus.value();
    }

    public String getStatusCodeText() {
        return httpStatus.getReasonPhrase();
    }

    private final ZonedDateTime errorDateTime = ZonedDateTime.now();
}