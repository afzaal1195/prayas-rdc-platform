package com.prayas.platform.tour;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * The raw token is shown to the school exactly once (API response body +
 * confirmation email) and never persisted. Only its SHA-256 hash is stored,
 * so a database leak alone can't be used to impersonate a school's status
 * link.
 */
@Component
public class TrackingTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    public record TokenPair(String rawToken, String hash) {
    }

    public TokenPair generate() {
        byte[] bytes = new byte[16]; // 128 bits
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new TokenPair(raw, sha256Hex(raw));
    }

    public String hash(String rawToken) {
        return sha256Hex(rawToken);
    }

    public boolean matches(String rawToken, String storedHash) {
        return MessageDigest.isEqual(
                sha256Hex(rawToken).getBytes(),
                storedHash.getBytes());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
