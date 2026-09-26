package com.ryvenca.auth;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.auth.AuthDtos.AuthResponse;
import com.ryvenca.auth.AuthDtos.FirebaseLoginRequest;
import com.ryvenca.auth.AuthDtos.LoginRequest;
import com.ryvenca.auth.AuthDtos.RegisterRequest;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.config.RyvencaProperties;
import com.ryvenca.firebase.FirebaseAuthGateway;
import com.ryvenca.firebase.FirebaseAuthGateway.VerifiedToken;
import com.ryvenca.user.AuthProvider;
import com.ryvenca.user.Role;
import com.ryvenca.user.User;
import com.ryvenca.user.UserDto;
import com.ryvenca.user.UserRepository;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final FirebaseAuthGateway firebase;
    private final boolean localEnabled;
    private final Set<String> adminEmails;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
                       FirebaseAuthGateway firebase, RyvencaProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.firebase = firebase;
        this.localEnabled = properties.auth() == null || properties.auth().localEnabled();
        List<String> admins = properties.admin() == null || properties.admin().emails() == null
                ? List.of() : properties.admin().emails();
        this.adminEmails = admins.stream().map(AuthService::normalize).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean localEnabled() {
        return localEnabled;
    }

    public boolean firebaseAvailable() {
        return firebase.available();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        requireLocal();
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT, "error.auth.emailTaken", Map.of("email", "validation.email.taken"));
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return respond(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        requireLocal();
        User user = users.findByEmail(normalize(request.email()))
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.badCredentials"));
        return respond(user);
    }

    /**
     * Exchanges a Firebase ID token (Apple, Google or e-mail sign-in) for a RYVENCA token. The account is
     * found by Firebase UID, else linked by e-mail, else created.
     */
    @Transactional
    public AuthResponse firebaseLogin(FirebaseLoginRequest request) {
        if (!firebase.available()) {
            throw new ApiException(ErrorCode.AUTH_UNAVAILABLE, "error.auth.firebaseUnavailable");
        }
        VerifiedToken token;
        try {
            token = firebase.verify(request.idToken());
        } catch (FirebaseAuthGateway.InvalidTokenException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.invalidToken");
        }
        AuthProvider provider = AuthProvider.fromFirebase(token.signInProvider());
        String email = token.email() == null || token.email().isBlank() ? null : normalize(token.email());
        if (email == null) {
            // Apple always shares an address (possibly a private relay); other providers too. Without one we
            // cannot contact or identify the user.
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "error.auth.emailRequired");
        }
        User user = users.findByFirebaseUid(token.uid()).orElse(null);
        if (user == null) {
            user = users.findByEmail(email).orElse(null);
            if (user != null) {
                // Same person signing in with another method: link, but only with a verified address.
                if (!token.emailVerified() && provider == AuthProvider.PASSWORD) {
                    throw new ApiException(ErrorCode.CONFLICT, "error.auth.emailTaken");
                }
                user.setFirebaseUid(token.uid());
                user.setAuthProvider(provider);
                log.info("Linked existing account {} to Firebase ({})", user.getId(), provider);
            } else {
                user = users.save(User.fromFirebase(email, token.uid(), provider, token.emailVerified(),
                        displayName(token, request.displayName(), email)));
            }
        }
        if (token.emailVerified()) {
            user.setEmailVerified(true);
        }
        return respond(user);
    }

    private AuthResponse respond(User user) {
        if (adminEmails.contains(user.getEmail()) && user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            log.info("Granted admin role to account {} (RYVENCA_ADMIN_EMAILS)", user.getId());
        }
        if (user.isDisabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "error.auth.accountDisabled");
        }
        TokenService.IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.token(), token.expiresAt(), UserDto.from(user));
    }

    private void requireLocal() {
        if (!localEnabled) {
            throw new ApiException(ErrorCode.LOCAL_AUTH_DISABLED, "error.auth.localDisabled");
        }
    }

    private static String displayName(VerifiedToken token, String requested, String email) {
        for (String candidate : new String[] {token.name(), requested}) {
            if (candidate != null && !candidate.isBlank()) {
                String trimmed = candidate.trim();
                return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
            }
        }
        String local = email.substring(0, email.indexOf('@'));
        return local.isEmpty() ? "RYVENCA" : local.substring(0, 1).toUpperCase(Locale.ROOT) + local.substring(1);
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
