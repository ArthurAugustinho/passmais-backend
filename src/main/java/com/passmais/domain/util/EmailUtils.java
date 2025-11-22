package com.passmais.domain.util;

import java.util.Locale;

/**
 * Utility helpers for handling e-mail normalization across the application.
 */
public final class EmailUtils {

    private EmailUtils() {
        // Utility class
    }

    /**
     * Normalizes e-mail addresses by trimming whitespace and converting to lower-case.
     *
     * @param email raw e-mail input
     * @return normalized e-mail or {@code null} if the result is empty/invalid
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
