package com.takarub.esim.identity.application.query;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Read-only query to load a session by identifier.
 */
public record GetSessionByIdQuery(String sessionId) {

    public GetSessionByIdQuery {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Session id is required");
        }
    }
}
