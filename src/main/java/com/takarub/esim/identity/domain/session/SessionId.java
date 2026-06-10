package com.takarub.esim.identity.domain.session;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link Session}. Immutable.
 */
public record SessionId(UUID value) {

    public SessionId {
        if (value == null) {
            throw new ValidationException("SessionId value must not be null");
        }
    }

    public static SessionId of(UUID value) {
        return new SessionId(value);
    }

    public static SessionId of(String value) {
        try {
            return new SessionId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("SessionId must be a valid UUID");
        }
    }

    public static SessionId generate(IdGenerator idGenerator) {
        return new SessionId(idGenerator.newUuid());
    }
}
