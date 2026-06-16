package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to log out the currently authenticated session.
 */
public record LogoutCommand(String sessionId) {

    public LogoutCommand {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Session id is required");
        }
    }
}
