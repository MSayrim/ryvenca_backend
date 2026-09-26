package com.ryvenca.deletion;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.i18n.Language;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public deletion request for people who can no longer sign in (linked from the web deletion page). */
@RestController
public class DeletionRequestController {

    public record DeletionRequestBody(
            @NotBlank(message = "{validation.email.required}") @Email(message = "{validation.email.invalid}")
            @Size(max = 254, message = "{validation.email.size}") String email,
            @Size(max = 1000, message = "{validation.message.size}") String message) {
    }

    public record DeletionRequestReceipt(String reference) {
    }

    private final AccountDeletionService deletion;
    private final RequestRateLimiter limiter;

    public DeletionRequestController(AccountDeletionService deletion, RequestRateLimiter limiter) {
        this.deletion = deletion;
        this.limiter = limiter;
    }

    @PostMapping("/api/account-deletion-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeletionRequestReceipt request(@Valid @RequestBody DeletionRequestBody body, HttpServletRequest http) {
        if (!limiter.allow("deletion:" + http.getRemoteAddr())) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS, "error.tooManyRequests");
        }
        String language = Language.of(LocaleContextHolder.getLocale()).code();
        return new DeletionRequestReceipt(deletion.request(body.email(), body.message(), language).getReference());
    }
}
