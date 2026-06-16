package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to refresh (rotate) a session's refresh token. References the session by identifier and
 * carries the presented refresh token to be validated against the stored one.
 */
public record RefreshSessionCommand(String sessionId, String refreshToken) {

    public RefreshSessionCommand {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Session id is required");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ValidationException("Refresh token is required");
        }
    }
}
