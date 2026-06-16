package com.takarub.esim.identity.domain.user;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link User}. Immutable.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new ValidationException("UserId value must not be null");
        }
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("UserId must be a valid UUID");
        }
    }

    public static UserId generate(IdGenerator idGenerator) {
        return new UserId(idGenerator.newUuid());
    }
}
