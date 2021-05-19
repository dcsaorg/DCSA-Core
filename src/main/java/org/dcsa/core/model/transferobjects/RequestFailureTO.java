package org.dcsa.core.model.transferobjects;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RequestFailureTO {

    private final String httpMethod;

    private final String requestUri;

    private final List<ConcreteRequestErrorMessageTO> errors;

    private final HttpStatus statusCode;

    public String getStatusCodeText() {
        return statusCode.getReasonPhrase();
    }

    private final ZonedDateTime errorDateTime = ZonedDateTime.now();
}