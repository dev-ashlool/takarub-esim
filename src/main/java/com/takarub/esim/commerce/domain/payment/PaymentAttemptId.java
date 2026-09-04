package com.takarub.esim.commerce.domain.payment;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link PaymentAttempt}. Immutable.
 */
public record PaymentAttemptId(UUID value) {

    public PaymentAttemptId {
        if (value == null) {
            throw new ValidationException("PaymentAttemptId value must not be null");
        }
    }

    public static PaymentAttemptId of(UUID value) {
        return new PaymentAttemptId(value);
    }

    public static PaymentAttemptId of(String value) {
        try {
            return new PaymentAttemptId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("PaymentAttemptId must be a valid UUID");
        }
    }

    public static PaymentAttemptId generate(IdGenerator idGenerator) {
        return new PaymentAttemptId(idGenerator.newUuid());
    }
}
