package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to checkout a single selected catalog package for the authenticated user.
 *
 * <p>Creates a fresh cart (canceling any prior OPEN cart), checks it out, and creates an Order in
 * {@code CREATED} status. Presentation supplies {@code userId} from the security context.
 */
public record CheckoutCommand(String userId, String packageId, int quantity) {

    public CheckoutCommand {
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
