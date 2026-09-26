package com.ryvenca.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.admin.AdminDtos.AdminUser;
import com.ryvenca.admin.AdminDtos.DeletionLogDto;
import com.ryvenca.admin.AdminDtos.DeletionRequestDto;
import com.ryvenca.admin.AdminDtos.Stats;
import com.ryvenca.admin.AdminDtos.UserPage;
import com.ryvenca.admin.AdminDtos.UserPatch;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.deletion.AccountDeletionService;
import com.ryvenca.deletion.DeletionLogRepository;
import com.ryvenca.deletion.DeletionMethod;
import com.ryvenca.deletion.DeletionRequest;
import com.ryvenca.deletion.DeletionRequestRepository;
import com.ryvenca.deletion.DeletionRequestStatus;
import com.ryvenca.garment.GarmentRepository;
import com.ryvenca.outfit.SavedOutfitRepository;
import com.ryvenca.user.User;
import com.ryvenca.user.UserRepository;

@Service
public class AdminService {

    private final UserRepository users;
    private final GarmentRepository garments;
    private final SavedOutfitRepository savedOutfits;
    private final DeletionRequestRepository requests;
    private final DeletionLogRepository deletionLog;
    private final AccountDeletionService deletion;
    private final Clock clock;

    public AdminService(UserRepository users, GarmentRepository garments, SavedOutfitRepository savedOutfits,
                        DeletionRequestRepository requests, DeletionLogRepository deletionLog,
                        AccountDeletionService deletion, Clock clock) {
        this.users = users;
        this.garments = garments;
        this.savedOutfits = savedOutfits;
        this.requests = requests;
        this.deletionLog = deletionLog;
        this.deletion = deletion;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        Instant now = clock.instant();
        return new Stats(users.count(), users.countByCreatedAtAfter(now.minus(Duration.ofDays(7))), garments.count(),
                savedOutfits.count(), requests.countByStatus(DeletionRequestStatus.PENDING),
                deletionLog.countByCreatedAtAfter(now.minus(Duration.ofDays(30))));
    }

    @Transactional(readOnly = true)
    public UserPage users(String q, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        Page<User> result = users.search(query, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100))));
        return new UserPage(result.getContent().stream().map(this::toDto).toList(), result.getTotalElements(),
                result.getNumber(), result.getSize());
    }

    @Transactional
    public AdminUser patchUser(long adminId, long userId, UserPatch patch) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("error.user.notFound"));
        if (userId == adminId) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "error.admin.self");
        }
        if (patch.role() != null) {
            user.setRole(patch.role());
        }
        if (patch.disabled() != null) {
            user.setDisabled(patch.disabled());
        }
        return toDto(user);
    }

    @Transactional
    public void deleteUser(long adminId, long userId) {
        if (userId == adminId) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "error.admin.self");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("error.user.notFound"));
        deletion.delete(user, DeletionMethod.ADMIN, null);
    }

    @Transactional(readOnly = true)
    public List<DeletionRequestDto> deletionRequests(DeletionRequestStatus status) {
        List<DeletionRequest> list = status == null ? requests.findAllByOrderByCreatedAtDesc()
                : requests.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public DeletionRequestDto approve(long adminId, long requestId, String note) {
        DeletionRequest request = pending(requestId);
        User user = users.findByEmail(request.getEmail()).orElse(null);
        if (user != null) {
            if (user.getId() == adminId) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "error.admin.self");
            }
            deletion.delete(user, DeletionMethod.REQUEST, request.getMessage());
        }
        request.resolve(DeletionRequestStatus.COMPLETED, blankToNull(note), adminId);
        return toDto(request);
    }

    @Transactional
    public DeletionRequestDto reject(long adminId, long requestId, String note) {
        DeletionRequest request = pending(requestId);
        request.resolve(DeletionRequestStatus.REJECTED, note.trim(), adminId);
        return toDto(request);
    }

    @Transactional(readOnly = true)
    public List<DeletionLogDto> deletionLog(int limit) {
        return deletionLog.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 500)))).stream()
                .map(e -> new DeletionLogDto(e.getMethod(), e.getAuthProvider(), e.getReason(), e.getCreatedAt()))
                .toList();
    }

    private DeletionRequest pending(long id) {
        DeletionRequest request = requests.findById(id)
                .orElseThrow(() -> ApiException.notFound("error.deletionRequest.notFound"));
        if (request.getStatus() != DeletionRequestStatus.PENDING) {
            throw new ApiException(ErrorCode.CONFLICT, "error.deletionRequest.resolved");
        }
        return request;
    }

    private AdminUser toDto(User u) {
        return new AdminUser(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getAuthProvider(),
                u.isDisabled(), u.getLanguage(), garments.countByOwnerId(u.getId()),
                savedOutfits.countByOwnerId(u.getId()), u.getCreatedAt());
    }

    private DeletionRequestDto toDto(DeletionRequest r) {
        Long matched = r.getStatus() == DeletionRequestStatus.PENDING
                ? users.findByEmail(r.getEmail()).map(User::getId).orElse(null) : null;
        return new DeletionRequestDto(r.getId(), r.getReference(), r.getEmail(), r.getMessage(), r.getLanguage(),
                r.getStatus(), matched, r.getCreatedAt(), r.getResolvedAt(), r.getResolvedBy(), r.getNote());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
