package com.takarub.esim.identity.domain.user;

import java.util.regex.Pattern;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * ISO-3166 alpha-2 country code stored on the customer profile.
 */
public final class CountryCode {

    private static final Pattern ISO2 = Pattern.compile("^[A-Z]{2}$");

    private CountryCode() {
    }

    public static String require(String raw) {
        String normalized = normalizeOptional(raw);
        if (normalized == null) {
            throw new ValidationException("Country is required");
        }
        return normalized;
    }

    public static String normalizeOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        if (!ISO2.matcher(normalized).matches()) {
            throw new ValidationException("Country must be an ISO-3166 alpha-2 code");
        }
        return normalized;
    }
}
