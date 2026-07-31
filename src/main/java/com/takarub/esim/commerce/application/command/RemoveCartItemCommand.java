package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to remove a line from the caller's open cart.
 */
public record RemoveCartItemCommand(String userId, String packageId) {

    public RemoveCartItemCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
        if (packageId == null || packageId.isBlank()) {
            throw new ValidationException("Package id is required");
        }
    }
}
