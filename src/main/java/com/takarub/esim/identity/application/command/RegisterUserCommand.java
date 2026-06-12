package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to register a new user. Carries raw inputs only; format validation of the e-mail and
 * hashing of the password happen downstream (domain value object and {@code PasswordHasher}).
 */
public record RegisterUserCommand(String email, String rawPassword) {

    public RegisterUserCommand {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password is required");
        }
    }
}
