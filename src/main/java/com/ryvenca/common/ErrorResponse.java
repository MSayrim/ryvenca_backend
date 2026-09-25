package com.ryvenca.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.status().value(), code.name(), message, Map.of());
    }

    public static ErrorResponse of(ErrorCode code, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(code.status().value(), code.name(), message, fieldErrors);
    }
}
