package com.takarub.esim.identity.domain.session;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Opaque refresh token value object. The domain treats the token as an opaque string; how it is
 * stored or transported is an infrastructure concern. Immutable.
 */
public record RefreshToken(String value) {

    public RefreshToken {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Refresh token must not be blank");
        }
    }

    public static RefreshToken of(String value) {
        return new RefreshToken(value);
    }

    public static RefreshToken generate(IdGenerator idGenerator) {
        return new RefreshToken(idGenerator.newId());
    }
}
