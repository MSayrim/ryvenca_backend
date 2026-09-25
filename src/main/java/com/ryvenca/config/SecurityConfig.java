package com.ryvenca.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.common.ErrorResponse;
import com.ryvenca.i18n.Language;
import com.ryvenca.i18n.LanguageLocaleResolver;
import com.ryvenca.i18n.Texts;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Texts texts) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/meta", "/media/**", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(unauthorized(texts)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized(texts))
                        .accessDeniedHandler(forbidden(texts)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtSecretKey(RyvencaProperties properties) {
        String secret = properties.security().jwtSecret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("ryvenca.security.jwt-secret must be at least 32 bytes (RYVENCA_JWT_SECRET)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(RyvencaProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static AuthenticationEntryPoint unauthorized(Texts texts) {
        return (request, response, ex) -> write(request, response, texts, ErrorCode.UNAUTHORIZED, "error.auth.required");
    }

    private static AccessDeniedHandler forbidden(Texts texts) {
        return (request, response, ex) -> write(request, response, texts, ErrorCode.FORBIDDEN, "error.forbidden");
    }

    /** Security errors happen before the DispatcherServlet, so the language is resolved here directly. */
    private static void write(HttpServletRequest request, HttpServletResponse response, Texts texts, ErrorCode code,
                              String messageKey) throws IOException {
        Language language = LanguageLocaleResolver.resolve(request.getHeader("Accept-Language"));
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(ErrorResponse.of(code, texts.of(language).t(messageKey))));
    }
}
