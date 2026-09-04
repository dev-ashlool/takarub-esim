package com.takarub.esim.commerce.domain.order;

import java.util.Optional;

import com.takarub.esim.commerce.domain.cart.CartId;

/**
 * Domain repository port for the {@link Order} aggregate. Implementations live in infrastructure
 * (out of scope for TASK-030).
 *
 * <p>Callers that create an order from a cart must uphold: at most one order per {@link CartId}.
 * Uniqueness is enforced by application orchestration and persistence constraints, not by this
 * aggregate alone.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    /**
     * Returns the order linked to the given cart, if any. Used for checkout idempotency
     * (one order per checked-out cart).
     */
    Optional<Order> findByCartId(CartId cartId);
}
