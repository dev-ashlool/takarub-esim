package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;

/**
 * Thrown when an open cart cannot be loaded for the requested user or cart id.
 */
public class CartNotFoundApplicationException extends ResourceNotFoundException {

    public CartNotFoundApplicationException(UserId userId) {
        super(CommerceApplicationErrorCode.CART_NOT_FOUND,
                "Open cart for user " + userId.value() + " was not found.");
    }

    public CartNotFoundApplicationException(CartId cartId) {
        super(CommerceApplicationErrorCode.CART_NOT_FOUND,
                "Cart " + cartId.value() + " was not found.");
    }
}
