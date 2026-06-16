package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to revoke a session, referenced by its identifier.
 */
public record RevokeSessionCommand(String sessionId) {

    public RevokeSessionCommand {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Session id is required");
        }
    }
}
