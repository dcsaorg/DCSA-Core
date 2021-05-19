package org.dcsa.core.model.transferobjects;

import lombok.Data;

@Data
public class ConcreteRequestErrorMessageTO {

    private final String reason;
    private final String message;
}
