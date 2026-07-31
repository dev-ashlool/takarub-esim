package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to add a catalog package to the caller's open cart (created if missing).
 */
public record AddItemToCartCommand(String userId, String packageId, int quantity) {

    public AddItemToCartCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
        if (packageId == null || packageId.isBlank()) {
            throw new ValidationException("Package id is required");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be positive");
        }
    }
}
