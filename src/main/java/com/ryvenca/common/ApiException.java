package com.ryvenca.common;

import java.util.Map;

/**
 * Business error. The message and the field error values are message keys, rendered in the request
 * language by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> fieldErrorKeys;

    public ApiException(ErrorCode code, String messageKey) {
        this(code, messageKey, Map.of());
    }

    public ApiException(ErrorCode code, String messageKey, Map<String, String> fieldErrorKeys) {
        super(messageKey);
        this.code = code;
        this.fieldErrorKeys = fieldErrorKeys;
    }

    public ErrorCode code() {
        return code;
    }

    public String messageKey() {
        return getMessage();
    }

    public Map<String, String> fieldErrorKeys() {
        return fieldErrorKeys;
    }

    public static ApiException notFound(String messageKey) {
        return new ApiException(ErrorCode.NOT_FOUND, messageKey);
    }

    public static ApiException invalidField(String field, String messageKey) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, messageKey, Map.of(field, messageKey));
    }
}
