package com.ryvenca.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.admin.AdminDtos.AdminUser;
import com.ryvenca.admin.AdminDtos.ApproveRequest;
import com.ryvenca.admin.AdminDtos.DeletionLogDto;
import com.ryvenca.admin.AdminDtos.DeletionRequestDto;
import com.ryvenca.admin.AdminDtos.RejectRequest;
import com.ryvenca.admin.AdminDtos.Stats;
import com.ryvenca.admin.AdminDtos.UserPage;
import com.ryvenca.admin.AdminDtos.UserPatch;
import com.ryvenca.auth.CurrentUser;
import com.ryvenca.deletion.DeletionRequestStatus;
import com.ryvenca.palette.PaletteService;
import com.ryvenca.palette.PaletteService.PaletteDto;
import com.ryvenca.palette.PaletteService.PaletteRequest;
import com.ryvenca.settings.SettingsDtos.AdminSettings;
import com.ryvenca.settings.SettingsDtos.SettingsUpdate;
import com.ryvenca.settings.SettingsService;
import com.ryvenca.user.UserService;

import jakarta.validation.Valid;

/** Admin panel API. Every call re-checks the admin role in the database. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService users;
    private final AdminService admin;
    private final SettingsService settings;
    private final PaletteService palettes;

    public AdminController(UserService users, AdminService admin, SettingsService settings, PaletteService palettes) {
        this.users = users;
        this.admin = admin;
        this.settings = settings;
        this.palettes = palettes;
    }

    @GetMapping("/stats")
    public Stats stats(@AuthenticationPrincipal Jwt jwt) {
        adminId(jwt);
        return admin.stats();
    }

    @GetMapping("/settings")
    public AdminSettings settings(@AuthenticationPrincipal Jwt jwt) {
        adminId(jwt);
        return settings.adminSettings();
    }

    @PutMapping("/settings")
    public AdminSettings updateSettings(@AuthenticationPrincipal Jwt jwt, @RequestBody SettingsUpdate update) {
        return settings.update(update, adminId(jwt));
    }

    @GetMapping("/users")
    public UserPage users(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        adminId(jwt);
        return admin.users(q, page, size);
    }

    @PatchMapping("/users/{id}")
    public AdminUser patchUser(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @RequestBody UserPatch patch) {
        return admin.patchUser(adminId(jwt), id, patch);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        admin.deleteUser(adminId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deletion-requests")
    public List<DeletionRequestDto> deletionRequests(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(required = false) DeletionRequestStatus status) {
        adminId(jwt);
        return admin.deletionRequests(status);
    }

    @PostMapping("/deletion-requests/{id}/approve")
    public DeletionRequestDto approve(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                      @Valid @RequestBody(required = false) ApproveRequest body) {
        return admin.approve(adminId(jwt), id, body == null ? null : body.note());
    }

    @PostMapping("/deletion-requests/{id}/reject")
    public DeletionRequestDto reject(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                     @Valid @RequestBody RejectRequest body) {
        return admin.reject(adminId(jwt), id, body.note());
    }

    @GetMapping("/deletion-log")
    public List<DeletionLogDto> deletionLog(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam(defaultValue = "100") int limit) {
        adminId(jwt);
        return admin.deletionLog(limit);
    }

    @GetMapping("/palettes")
    public List<PaletteDto> palettes(@AuthenticationPrincipal Jwt jwt) {
        adminId(jwt);
        return palettes.list();
    }

    @PostMapping("/palettes")
    @ResponseStatus(HttpStatus.CREATED)
    public PaletteDto createPalette(@AuthenticationPrincipal Jwt jwt, @RequestBody PaletteRequest request) {
        adminId(jwt);
        return palettes.create(request);
    }

    @PutMapping("/palettes/{id}")
    public PaletteDto updatePalette(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
                                    @RequestBody PaletteRequest request) {
        adminId(jwt);
        return palettes.update(id, request);
    }

    @DeleteMapping("/palettes/{id}")
    public ResponseEntity<Void> deletePalette(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        adminId(jwt);
        palettes.delete(id);
        return ResponseEntity.noContent().build();
    }

    private long adminId(Jwt jwt) {
        return users.requireAdmin(CurrentUser.id(jwt)).getId();
    }
}
