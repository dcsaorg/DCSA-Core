package org.dcsa.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import org.dcsa.core.model.transferobjects.ConcreteRequestErrorMessageTO;

public abstract class ConcreteRequestErrorMessageException extends DCSAException {

    @Getter
    private final String reason;

    @Getter
    private final Object reference;


    protected ConcreteRequestErrorMessageException(String reason, Object reference, String message) {
        super(message);
        this.reference = reference;
        this.reason = reason;
    }

    protected ConcreteRequestErrorMessageException(String reason, Object reference, String message, Throwable cause) {
        super(message, cause);
        this.reference = reference;
        this.reason = reason;
    }

    public ConcreteRequestErrorMessageTO asConcreteRequestMessage() {
        return new ConcreteRequestErrorMessageTO(getReason(), getMessage());
    }

    public static ConcreteRequestErrorMessageException notFound(String message) {
        return notFound(message, null);
    }

    public static ConcreteRequestErrorMessageException notFound(String message, Throwable cause) {
        return new NotFoundException("notFound", null, message, cause);
    }

    public static ConcreteRequestErrorMessageException invalidInput(String message, Throwable cause) {
        return new BadRequestException("invalidInput", null, message, cause);
    }

    public static ConcreteRequestErrorMessageException invalidQuery(String queryParameter, String message) {
        return invalidQuery(queryParameter, message, null);
    }

    public static ConcreteRequestErrorMessageException invalidQuery(String queryParameter, String message, Throwable cause) {
        return new BadRequestException("invalidQuery", queryParameter, message, cause);
    }

    public static ConcreteRequestErrorMessageException invalidParameter(String message) {
        return invalidParameter(null, null, message, null);
    }

    public static ConcreteRequestErrorMessageException invalidParameter(String message, Throwable cause) {
        return new BadRequestException("invalidParameter", null, message, cause);
    }

    public static ConcreteRequestErrorMessageException invalidParameter(String entityName, String attributePath, String message) {
        return invalidParameter(entityName, attributePath, message, null);
    }

    public static ConcreteRequestErrorMessageException invalidParameter(String entityName, String attributePath, String message, Throwable cause) {
        return new BadRequestException("invalidParameter", AttributeReference.of(entityName, attributePath), message, cause);
    }

    public static ConcreteRequestErrorMessageException internalServerError(String message) {
        return internalServerError(message, null);
    }

    public static ConcreteRequestErrorMessageException internalServerError(String message, Throwable cause) {
        return new InternalServerErrorException("internalError", null, message, cause);
    }

    @Data
    private static class AttributeReference {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private final String attributeName;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private final String entityName;

        static AttributeReference of(String attributeName, String entityName) {
            if (attributeName == null && entityName == null) {
                return null;
            }
            return new AttributeReference(attributeName, entityName);
        }
    }
}
