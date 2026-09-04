package com.takarub.esim.commerce.domain.order;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of an {@link Order}. Immutable.
 */
public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new ValidationException("OrderId value must not be null");
        }
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId of(String value) {
        try {
            return new OrderId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("OrderId must be a valid UUID");
        }
    }

    public static OrderId generate(IdGenerator idGenerator) {
        return new OrderId(idGenerator.newUuid());
    }
}
