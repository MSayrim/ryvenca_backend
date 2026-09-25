package com.ryvenca.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ryvenca.i18n.Texts;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Texts texts;

    public GlobalExceptionHandler(Texts texts) {
        this.texts = texts;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.fieldErrorKeys().forEach((field, key) -> fields.put(field, texts.t(key)));
        return ResponseEntity.status(ex.code().status())
                .body(ErrorResponse.of(ex.code(), texts.t(ex.messageKey()), fields));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String message = fields.isEmpty() ? texts.t("error.validation") : fields.values().iterator().next();
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message, fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return respond(ErrorCode.VALIDATION_ERROR, "error.badRequest", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return respond(ErrorCode.PAYLOAD_TOO_LARGE, "error.image.tooLarge", Map.of());
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return respond(ErrorCode.INVALID_IMAGE, "error.image.uploadFailed", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return respond(ErrorCode.FORBIDDEN, "error.forbidden", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return respond(ErrorCode.NOT_FOUND, "error.notFound", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return respond(ErrorCode.NOT_FOUND, "error.notFound", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "error.internal", Map.of());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String messageKey, Map<String, String> fields) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, texts.t(messageKey), fields));
    }
}
