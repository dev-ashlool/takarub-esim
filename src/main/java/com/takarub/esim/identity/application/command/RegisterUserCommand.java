package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to register a new user. Carries raw inputs only; format validation of the e-mail and
 * hashing of the password happen downstream (domain value object and {@code PasswordHasher}).
 */
public record RegisterUserCommand(
        String email,
        String rawPassword,
        String fullName,
        String country,
        String phoneNumber) {

    public RegisterUserCommand {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (country == null || country.isBlank()) {
            throw new ValidationException("Country is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ValidationException("Phone number is required");
        }
    }
}
