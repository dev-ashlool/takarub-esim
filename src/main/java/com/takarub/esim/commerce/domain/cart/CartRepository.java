package com.takarub.esim.commerce.domain.cart;

import java.util.Optional;

import com.takarub.esim.identity.domain.user.UserId;

/**
 * Domain repository port for the {@link Cart} aggregate. Implementations live in infrastructure
 * (out of scope for TASK-026).
 *
 * <p>Callers that create or load a cart must uphold: at most one {@link CartStatus#OPEN} cart per
 * {@link UserId}.
 */
public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findById(CartId cartId);

    /**
     * Returns the open cart for the user, if any. Used to enforce one OPEN cart per user.
     */
    Optional<Cart> findOpenByUserId(UserId userId);
}
