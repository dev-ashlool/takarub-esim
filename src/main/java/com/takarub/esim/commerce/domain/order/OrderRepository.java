package com.takarub.esim.commerce.domain.order;

import java.util.List;
import java.util.Optional;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Domain repository port for the {@link Order} aggregate. Implementations live in infrastructure.
 *
 * <p>Callers that create an order from a cart must uphold: at most one order per {@link CartId}.
 * Uniqueness is enforced by application orchestration and persistence constraints, not by this
 * aggregate alone.
 *
 * <p>Checkout idempotency: at most one order per ({@link UserId}, {@link CheckoutRequestId}).
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    /**
     * Returns the order linked to the given cart, if any. Used for one-order-per-cart invariant.
     */
    Optional<Order> findByCartId(CartId cartId);

    /**
     * Returns the order for the given user and client checkout request id, if any. Used for
     * checkout idempotency (same intentional action retries).
     */
    Optional<Order> findByUserIdAndCheckoutRequestId(UserId userId, CheckoutRequestId checkoutRequestId);

    /**
     * Returns all orders for the user, newest {@code createdAt} first. Used by customer My Orders.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(UserId userId);
}
