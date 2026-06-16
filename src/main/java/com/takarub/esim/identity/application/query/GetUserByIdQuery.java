package com.takarub.esim.identity.application.query;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Read-only query to load a user by identifier.
 */
public record GetUserByIdQuery(String userId) {

    public GetUserByIdQuery {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
    }
}
