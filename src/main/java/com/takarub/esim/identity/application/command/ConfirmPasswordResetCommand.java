package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to confirm a password reset. References the password-reset verification by identifier and
 * carries the new raw password (hashed downstream by {@code PasswordHasher}).
 */
public record ConfirmPasswordResetCommand(String verificationId, String newRawPassword) {

    public ConfirmPasswordResetCommand {
        if (verificationId == null || verificationId.isBlank()) {
            throw new ValidationException("Verification id is required");
        }
        if (newRawPassword == null || newRawPassword.isBlank()) {
            throw new ValidationException("New password is required");
        }
    }
}
