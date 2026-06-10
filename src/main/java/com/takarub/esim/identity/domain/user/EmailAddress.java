package com.takarub.esim.identity.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Email address value object. Normalized to lower case and validated on construction. Immutable.
 */
public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Email address must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("Email address format is invalid");
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
}
