package com.ryvenca.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static long id(Jwt jwt) {
        if (jwt == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.required");
        }
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.invalidSession");
        }
    }
}
