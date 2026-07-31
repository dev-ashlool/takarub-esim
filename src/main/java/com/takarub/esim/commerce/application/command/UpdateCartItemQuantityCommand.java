package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to replace the quantity of an existing line on the caller's open cart.
 */
public record UpdateCartItemQuantityCommand(String userId, String packageId, int quantity) {

    public UpdateCartItemQuantityCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
        if (packageId == null || packageId.isBlank()) {
            throw new ValidationException("Package id is required");
        }
    }
}
