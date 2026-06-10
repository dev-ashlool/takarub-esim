package com.takarub.esim.identity.domain.verification;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link Verification}. Immutable.
 */
public record VerificationId(UUID value) {

    public VerificationId {
        if (value == null) {
            throw new ValidationException("VerificationId value must not be null");
        }
    }

    public static VerificationId of(UUID value) {
        return new VerificationId(value);
    }

    public static VerificationId of(String value) {
        try {
            return new VerificationId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("VerificationId must be a valid UUID");
        }
    }

    public static VerificationId generate(IdGenerator idGenerator) {
        return new VerificationId(idGenerator.newUuid());
    }
}
