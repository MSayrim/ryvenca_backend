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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return respond(ex.code(), ex.getMessage(), ex.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String message = fields.isEmpty() ? "Gönderilen bilgiler geçersiz." : fields.values().iterator().next();
        return respond(ErrorCode.VALIDATION_ERROR, message, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return respond(ErrorCode.VALIDATION_ERROR, "İstek okunamadı. Lütfen bilgileri kontrol et.", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return respond(ErrorCode.PAYLOAD_TOO_LARGE, "Fotoğraf çok büyük. En fazla 15 MB yükleyebilirsin.", Map.of());
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return respond(ErrorCode.INVALID_IMAGE, "Fotoğraf yüklenemedi. Lütfen tekrar dene.", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return respond(ErrorCode.FORBIDDEN, "Bu işlem için yetkin yok.", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return respond(ErrorCode.NOT_FOUND, "İstenen kaynak bulunamadı.", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return respond(ErrorCode.NOT_FOUND, "İstenen kaynak bulunamadı.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "Beklenmeyen bir hata oluştu. Lütfen tekrar dene.", Map.of());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, Map<String, String> fields) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message, fields));
    }
}
