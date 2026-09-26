package com.ryvenca.user;

import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.i18n.Language;
import com.ryvenca.deletion.AccountDeletionService;
import com.ryvenca.deletion.DeletionMethod;

@Service
public class UserService {

    private final UserRepository users;
    private final AccountDeletionService deletion;

    public UserService(UserRepository users, AccountDeletionService deletion) {
        this.users = users;
        this.deletion = deletion;
    }

    /**
     * Resolves the authenticated user; a token of a deleted account is treated as unauthenticated and a
     * disabled account is refused.
     */
    @Transactional(readOnly = true)
    public User require(long userId) {
        User user = requireEvenIfDisabled(userId);
        if (user.isDisabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "error.auth.accountDisabled");
        }
        return user;
    }

    /** Like {@link #require} but also returns disabled accounts (they may still delete themselves). */
    @Transactional(readOnly = true)
    public User requireEvenIfDisabled(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.sessionExpired"));
    }

    /** Resolves an admin; everyone else gets 403. */
    @Transactional(readOnly = true)
    public User requireAdmin(long userId) {
        User user = require(userId);
        if (!user.isAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "error.forbidden");
        }
        return user;
    }

    @Transactional
    public User update(long userId, UpdateProfileRequest request) {
        User user = require(userId);
        if (request.displayName() != null) {
            String name = request.displayName().trim();
            if (name.isEmpty()) {
                throw ApiException.invalidField("displayName", "validation.name.blank");
            }
            user.setDisplayName(name);
        }
        if (request.wardrobeType() != null) {
            user.setWardrobeType(request.wardrobeType());
        }
        if (request.stylePreferences() != null) {
            user.setStylePreferences(request.stylePreferences().isEmpty()
                    ? EnumSet.noneOf(com.ryvenca.catalog.StylePreference.class)
                    : EnumSet.copyOf(request.stylePreferences()));
        }
        if (request.language() != null) {
            Language language = Language.fromTag(request.language())
                    .orElseThrow(() -> ApiException.invalidField("language", "validation.language.unsupported"));
            user.setLanguage(language.code());
        }
        if (request.onboardingCompleted() != null) {
            user.setOnboardingCompleted(request.onboardingCompleted());
        }
        return user;
    }

    /** Self-service deletion (app or website); also allowed for disabled accounts. */
    @Transactional
    public void delete(long userId, DeletionMethod method, String reason) {
        deletion.delete(requireEvenIfDisabled(userId), method, reason);
    }
}
