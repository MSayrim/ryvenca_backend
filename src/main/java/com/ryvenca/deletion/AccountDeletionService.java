package com.ryvenca.deletion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.config.RyvencaProperties;
import com.ryvenca.firebase.FirebaseAuthGateway;
import com.ryvenca.image.ImageService;
import com.ryvenca.user.User;
import com.ryvenca.user.UserRepository;

/**
 * Permanently deletes accounts (store review requirement: users can delete their account and data from
 * the app and from the web). Everything goes: photos, garments, saved outfits, the account row and the
 * Firebase Authentication user. Only an anonymized log entry remains.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
    private static final String REFERENCE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final UserRepository users;
    private final ImageService images;
    private final FirebaseAuthGateway firebase;
    private final DeletionLogRepository deletionLog;
    private final DeletionRequestRepository requests;
    private final byte[] hashSalt;
    private final SecureRandom random = new SecureRandom();

    public AccountDeletionService(UserRepository users, ImageService images, FirebaseAuthGateway firebase,
                                  DeletionLogRepository deletionLog, DeletionRequestRepository requests,
                                  RyvencaProperties properties) {
        this.users = users;
        this.images = images;
        this.firebase = firebase;
        this.deletionLog = deletionLog;
        this.requests = requests;
        this.hashSalt = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void delete(User user, DeletionMethod method, String reason) {
        String uid = user.getFirebaseUid();
        images.deleteAllFilesOf(user.getId());
        deletionLog.save(new DeletionLogEntry(hash(user.getEmail()), user.getAuthProvider(), method, trim(reason, 1000)));
        users.delete(user);
        users.flush();
        if (uid != null) {
            try {
                firebase.deleteUser(uid);
            } catch (RuntimeException e) {
                // The account data is gone; a stale Firebase user can only sign in to a fresh, empty account.
                log.error("Firebase user of deleted account could not be removed", e);
            }
        }
        log.info("Account {} deleted ({})", user.getId(), method);
    }

    /**
     * Records a deletion request from someone who cannot sign in. Always succeeds, so the response
     * doesn't reveal whether an account exists; a pending request for the same e-mail is reused.
     */
    @Transactional
    public DeletionRequest request(String email, String message, String language) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return requests.findFirstByEmailAndStatus(normalized, DeletionRequestStatus.PENDING)
                .orElseGet(() -> requests.save(new DeletionRequest(newReference(), normalized, trim(message, 1000), language)));
    }

    String hash(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(hashSalt);
            return HexFormat.of().formatHex(digest.digest(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String newReference() {
        String reference;
        do {
            StringBuilder sb = new StringBuilder("DR-");
            for (int i = 0; i < 6; i++) {
                sb.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
            }
            reference = sb.toString();
        } while (requests.existsByReference(reference));
        return reference;
    }

    private static String trim(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String t = value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
