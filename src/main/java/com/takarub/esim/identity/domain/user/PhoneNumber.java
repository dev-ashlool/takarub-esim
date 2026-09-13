package com.takarub.esim.identity.domain.user;

import java.util.regex.Pattern;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Customer phone number. Accepts digits with an optional leading plus.
 */
public final class PhoneNumber {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{8,18}$");

    private PhoneNumber() {
    }

    public static String require(String raw) {
        String normalized = normalizeOptional(raw);
        if (normalized == null) {
            throw new ValidationException("Phone number is required");
        }
        return normalized;
    }

    public static String normalizeOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().replace(" ", "");
        if (!PHONE.matcher(normalized).matches()) {
            throw new ValidationException("Phone number must be 8-18 digits with an optional leading +");
        }
        return normalized;
    }
}
