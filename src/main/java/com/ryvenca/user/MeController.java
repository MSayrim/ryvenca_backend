package com.ryvenca.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.auth.CurrentUser;

import jakarta.validation.Valid;

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

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        userService.delete(CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
