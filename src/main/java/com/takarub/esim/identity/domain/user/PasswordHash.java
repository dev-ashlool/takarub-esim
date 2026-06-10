package com.takarub.esim.identity.domain.user;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Holds an already-hashed password. The domain never hashes or verifies raw passwords; hashing is
 * an infrastructure concern. Immutable.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Password hash must not be blank");
        }
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }
}
