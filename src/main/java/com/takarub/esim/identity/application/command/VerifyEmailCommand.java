package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to verify a user's e-mail. References the verification by its identifier (the opaque
 * token is delivered out-of-band and is not stored on the aggregate).
 */
public record VerifyEmailCommand(String verificationId) {

    public VerifyEmailCommand {
        if (verificationId == null || verificationId.isBlank()) {
            throw new ValidationException("Verification id is required");
        }
    }
}
