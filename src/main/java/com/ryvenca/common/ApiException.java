package com.ryvenca.common;

import java.util.Map;

/** Business error with a user-presentable Turkish message. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> fieldErrors;

    public ApiException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ApiException(ErrorCode code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }

    public static ApiException invalidField(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message, Map.of(field, message));
    }
}
