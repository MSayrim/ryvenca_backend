package com.ryvenca.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.auth.CurrentUser;
import com.ryvenca.deletion.DeletionMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return UserDto.from(userService.require(CurrentUser.id(jwt)));
    }

    @PutMapping
    public UserDto update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return UserDto.from(userService.update(CurrentUser.id(jwt), request));
    }

    /** Optional body for {@code DELETE /api/me}. */
    public record DeleteAccountRequest(@Size(max = 1000, message = "{validation.message.size}") String reason) {
    }

    /**
     * Permanently deletes the account. Mobile clients send {@code X-Client: mobile} so the anonymized log can
     * tell in-app deletions from website deletions.
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody(required = false) DeleteAccountRequest request,
                                       @RequestHeader(value = "X-Client", required = false) String client) {
        DeletionMethod method = client != null && client.toLowerCase(java.util.Locale.ROOT).startsWith("mobile")
                ? DeletionMethod.IN_APP : DeletionMethod.WEB;
        userService.delete(CurrentUser.id(jwt), method, request == null ? null : request.reason());
        return ResponseEntity.noContent().build();
    }
}
