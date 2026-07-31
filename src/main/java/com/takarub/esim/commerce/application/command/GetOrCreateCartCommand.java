package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to load the caller's open cart, creating and persisting an empty one when none exists.
 */
public record GetOrCreateCartCommand(String userId) {

    public GetOrCreateCartCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
    }
}
