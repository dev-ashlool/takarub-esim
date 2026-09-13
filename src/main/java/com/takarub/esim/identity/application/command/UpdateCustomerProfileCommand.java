package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

public record UpdateCustomerProfileCommand(
        String userId,
        String fullName,
        String country,
        String phoneNumber) {

    public UpdateCustomerProfileCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
        if (country == null || country.isBlank()) {
            throw new ValidationException("Country is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ValidationException("Phone number is required");
        }
    }
}
