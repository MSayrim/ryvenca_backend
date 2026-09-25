package com.ryvenca.auth;

import java.time.Instant;

import com.ryvenca.user.UserDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "E-posta gerekli") @Email(message = "Geçerli bir e-posta gir")
            @Size(max = 254, message = "E-posta çok uzun") String email,
            @NotBlank(message = "Şifre gerekli") @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalı")
            String password,
            @NotBlank(message = "İsim gerekli") @Size(max = 80, message = "İsim en fazla 80 karakter olabilir")
            String displayName) {
    }

    public record LoginRequest(
            @NotBlank(message = "E-posta gerekli") String email,
            @NotBlank(message = "Şifre gerekli") String password) {
    }

    public record AuthResponse(String token, Instant expiresAt, UserDto user) {
    }
}
