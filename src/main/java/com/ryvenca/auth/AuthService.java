package com.ryvenca.auth;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.auth.AuthDtos.AuthResponse;
import com.ryvenca.auth.AuthDtos.LoginRequest;
import com.ryvenca.auth.AuthDtos.RegisterRequest;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.user.User;
import com.ryvenca.user.UserDto;
import com.ryvenca.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT, "Bu e-posta ile kayıtlı bir hesap zaten var.",
                    java.util.Map.of("email", "Bu e-posta zaten kullanılıyor"));
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "E-posta veya şifre hatalı."));
        return respond(user);
    }

    private AuthResponse respond(User user) {
        TokenService.IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.token(), token.expiresAt(), UserDto.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
