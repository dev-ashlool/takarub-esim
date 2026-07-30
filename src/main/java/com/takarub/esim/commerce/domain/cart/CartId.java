package com.takarub.esim.commerce.domain.cart;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link Cart}. Immutable.
 */
public record CartId(UUID value) {

    public CartId {
        if (value == null) {
            throw new ValidationException("CartId value must not be null");
        }
    }

    public static CartId of(UUID value) {
        return new CartId(value);
    }

    public static CartId of(String value) {
        try {
            return new CartId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("CartId must be a valid UUID");
        }
    }

    public static CartId generate(IdGenerator idGenerator) {
        return new CartId(idGenerator.newUuid());
    }
}
