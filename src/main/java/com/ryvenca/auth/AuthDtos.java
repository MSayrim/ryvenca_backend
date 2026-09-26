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
            @NotBlank(message = "{validation.email.required}") @Email(message = "{validation.email.invalid}")
            @Size(max = 254, message = "{validation.email.size}") String email,
            @NotBlank(message = "{validation.password.required}") @Size(min = 8, max = 100, message = "{validation.password.size}")
            String password,
            @NotBlank(message = "{validation.name.required}") @Size(max = 80, message = "{validation.name.size}")
            String displayName) {
    }

    public record LoginRequest(
            @NotBlank(message = "{validation.email.required}") String email,
            @NotBlank(message = "{validation.password.required}") String password) {
    }

    public record FirebaseLoginRequest(
            @NotBlank(message = "{validation.idToken.required}") @Size(max = 8192) String idToken,
            @Size(max = 80, message = "{validation.name.size}") String displayName) {
    }

    public record AuthResponse(String token, Instant expiresAt, UserDto user) {
    }
}
