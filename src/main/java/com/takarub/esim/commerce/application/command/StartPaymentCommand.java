package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to start or reuse payment for an existing Order owned by the authenticated user.
 *
 * <p>Presentation supplies {@code userId} from the security context. Does not accept amount,
 * currency, or provider data — those come from the Order snapshot and future provider integration.
 */
public record StartPaymentCommand(String userId, String orderId) {

    public StartPaymentCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new ValidationException("Order id is required");
        }
    }
}
