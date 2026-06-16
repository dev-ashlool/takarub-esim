package com.takarub.esim.identity.application.query;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Read-only query to load a user by e-mail address.
 */
public record GetUserByEmailQuery(String email) {

    public GetUserByEmailQuery {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
    }
}
