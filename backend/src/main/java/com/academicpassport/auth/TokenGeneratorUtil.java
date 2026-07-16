package com.academicpassport.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGeneratorUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenGeneratorUtil() {}

    /**
     * Generates a secure random URL-safe Base64 token with 32 bytes of entropy.
     */
    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
