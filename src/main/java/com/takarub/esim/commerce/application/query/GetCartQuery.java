package com.takarub.esim.commerce.application.query;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Read-only query to load the caller's open cart without creating one.
 */
public record GetCartQuery(String userId) {

    public GetCartQuery {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
    }
}
