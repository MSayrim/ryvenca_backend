package com.ryvenca.admin;

import java.time.Instant;
import java.util.List;

import com.ryvenca.deletion.DeletionMethod;
import com.ryvenca.deletion.DeletionRequestStatus;
import com.ryvenca.user.AuthProvider;
import com.ryvenca.user.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record Stats(long users, long newUsers7d, long garments, long savedOutfits, long pendingDeletionRequests,
                        long deletionsLast30d) {
    }

    public record AdminUser(Long id, String email, String displayName, Role role, AuthProvider authProvider,
                            boolean disabled, String language, long garmentCount, long savedOutfitCount,
                            Instant createdAt) {
    }

    public record UserPage(List<AdminUser> items, long total, int page, int size) {
    }

    public record UserPatch(Role role, Boolean disabled) {
    }

    public record DeletionRequestDto(Long id, String reference, String email, String message, String language,
                                     DeletionRequestStatus status, Long matchedUserId, Instant createdAt,
                                     Instant resolvedAt, Long resolvedBy, String note) {
    }

    public record ApproveRequest(@Size(max = 1000, message = "{validation.message.size}") String note) {
    }

    public record RejectRequest(
            @NotBlank(message = "{validation.note.required}") @Size(max = 1000, message = "{validation.message.size}")
            String note) {
    }

    public record DeletionLogDto(DeletionMethod method, AuthProvider authProvider, String reason, Instant createdAt) {
    }
}
