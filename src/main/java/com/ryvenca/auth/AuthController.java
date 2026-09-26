package com.ryvenca.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.auth.AuthDtos.AuthResponse;
import com.ryvenca.auth.AuthDtos.FirebaseLoginRequest;
import com.ryvenca.auth.AuthDtos.LoginRequest;
import com.ryvenca.auth.AuthDtos.RegisterRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Apple / Google / e-mail sign-in through Firebase. */
    @PostMapping("/firebase")
    public AuthResponse firebase(@Valid @RequestBody FirebaseLoginRequest request) {
        return authService.firebaseLogin(request);
    }
}
