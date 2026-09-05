package com.takarub.esim.commerce.domain.fulfillment;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link FulfillmentWork}. Immutable.
 */
public record FulfillmentId(UUID value) {

    public FulfillmentId {
        if (value == null) {
            throw new ValidationException("FulfillmentId value must not be null");
        }
    }

    public static FulfillmentId of(UUID value) {
        return new FulfillmentId(value);
    }

    public static FulfillmentId of(String value) {
        try {
            return new FulfillmentId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("FulfillmentId must be a valid UUID");
        }
    }

    public static FulfillmentId generate(IdGenerator idGenerator) {
        return new FulfillmentId(idGenerator.newUuid());
    }
}
