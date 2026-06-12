package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to request a password reset for the account owning the supplied e-mail.
 */
public record RequestPasswordResetCommand(String email) {

    public RequestPasswordResetCommand {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
    }
}
