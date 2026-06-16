package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to authenticate a user with e-mail and password and start a new session.
 */
public record AuthenticateUserCommand(
        String email,
        String password,
        AuthenticateUserDeviceMetadata deviceMetadata
) {

    public AuthenticateUserCommand {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (deviceMetadata == null) {
            throw new ValidationException("Device metadata is required");
        }
    }
}
