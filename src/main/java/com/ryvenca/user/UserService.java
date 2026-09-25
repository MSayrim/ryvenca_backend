package com.ryvenca.user;

import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.image.ImageService;

@Service
public class UserService {

    private final UserRepository users;
    private final ImageService images;

    public UserService(UserRepository users, ImageService images) {
        this.users = users;
        this.images = images;
    }

    /** Resolves the authenticated user; a token of a deleted account is treated as unauthenticated. */
    @Transactional(readOnly = true)
    public User require(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Oturumun sona erdi. Lütfen tekrar giriş yap."));
    }

    @Transactional
    public User update(long userId, UpdateProfileRequest request) {
        User user = require(userId);
        if (request.displayName() != null) {
            String name = request.displayName().trim();
            if (name.isEmpty()) {
                throw ApiException.invalidField("displayName", "İsim boş olamaz");
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
        if (request.onboardingCompleted() != null) {
            user.setOnboardingCompleted(request.onboardingCompleted());
        }
        return user;
    }

    /** Deletes the account and everything that belongs to it (DB rows cascade, files are removed here). */
    @Transactional
    public void delete(long userId) {
        User user = require(userId);
        images.deleteAllFilesOf(user.getId());
        users.delete(user);
    }
}
