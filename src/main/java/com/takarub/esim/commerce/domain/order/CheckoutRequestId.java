package com.takarub.esim.commerce.domain.order;

import java.util.Objects;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Client-generated idempotency key for one intentional checkout action. Immutable. Not produced by
 * {@link com.takarub.esim.identity.shared.id.IdGenerator}. Stored exactly as supplied (no trim or
 * case normalization).
 */
public record CheckoutRequestId(String value) {

    public static final int MAX_LENGTH = 36;

    public CheckoutRequestId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Checkout request id is required");
        }
        if (value.length() > MAX_LENGTH) {
            throw new ValidationException("Checkout request id must be at most " + MAX_LENGTH + " characters");
        }
    }

    public static CheckoutRequestId of(String value) {
        return new CheckoutRequestId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CheckoutRequestId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
