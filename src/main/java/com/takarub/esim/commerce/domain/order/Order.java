package com.takarub.esim.commerce.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Order aggregate root. Owns a frozen commercial snapshot for one checked-out cart and the
 * payment-related lifecycle states. Does not call Cart, Catalog, Pricing, or payment providers;
 * the application supplies {@link OrderItemSnapshot} lines and drives transitions.
 */
public class Order {

    private static final int MONEY_SCALE = 2;

    private final OrderId id;
    private final CartId cartId;
    private final UserId userId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final String currency;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(OrderId id, Instant createdAt, Instant updatedAt, CartId cartId, UserId userId,
                  OrderStatus status, List<OrderItem> items, BigDecimal totalAmount, String currency) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.cartId = Objects.requireNonNull(cartId, "cartId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.items = new ArrayList<>(items);
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
    }

    /**
     * Creates a new {@link OrderStatus#CREATED} order from commercial line snapshots.
     */
    public static Order create(IdGenerator idGenerator, ClockProvider clock, CartId cartId,
                               UserId userId, List<OrderItemSnapshot> lines) {
        if (cartId == null) {
            throw new ValidationException("Cart id is required to create an order");
        }
        if (userId == null) {
            throw new ValidationException("User id is required to create an order");
        }
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        List<OrderItem> orderItems = new ArrayList<>(lines.size());
        for (OrderItemSnapshot line : lines) {
            orderItems.add(new OrderItem(line));
        }

        String currency = orderItems.get(0).currency();
        for (OrderItem item : orderItems) {
            if (!currency.equals(item.currency())) {
                throw new ValidationException("All order items must use the same currency");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            total = total.add(item.lineTotal());
        }
        total = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        Instant now = clock.now();
        return new Order(
                OrderId.generate(idGenerator),
                now,
                now,
                cartId,
                userId,
                OrderStatus.CREATED,
                orderItems,
                total,
                currency);
    }

    /**
     * Rebuilds an order from persisted state without running create rules. For exclusive use by
     * the infrastructure persistence mapper.
     */
    public static Order reconstitute(OrderId id, Instant createdAt, Instant updatedAt, CartId cartId,
                                     UserId userId, OrderStatus status, List<OrderItem> items,
                                     BigDecimal totalAmount, String currency) {
        return new Order(
                id,
                createdAt,
                updatedAt,
                cartId,
                userId,
                status,
                items == null ? List.of() : items,
                totalAmount,
                currency);
    }

    /**
     * Transitions {@link OrderStatus#CREATED} or {@link OrderStatus#PAYMENT_FAILED} to
     * {@link OrderStatus#PENDING_PAYMENT}.
     */
    public void startPayment(ClockProvider clock) {
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid and cannot be changed");
        }
        if (status == OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order is already pending payment");
        }
        if (status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_FAILED) {
            this.status = OrderStatus.PENDING_PAYMENT;
            touch(clock);
            return;
        }
        throw new ConflictException("Invalid order state for startPayment: " + status);
    }

    /**
     * Transitions {@link OrderStatus#PENDING_PAYMENT} to {@link OrderStatus#PAID}.
     */
    public void markPaid(ClockProvider clock) {
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid");
        }
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order must be pending payment to mark as paid");
        }
        this.status = OrderStatus.PAID;
        touch(clock);
    }

    /**
     * Transitions {@link OrderStatus#PENDING_PAYMENT} to {@link OrderStatus#PAYMENT_FAILED}.
     */
    public void markPaymentFailed(ClockProvider clock) {
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Paid order cannot be marked as payment failed");
        }
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order must be pending payment to mark payment as failed");
        }
        this.status = OrderStatus.PAYMENT_FAILED;
        touch(clock);
    }

    public List<OrderItem> itemsView() {
        return Collections.unmodifiableList(items);
    }

    public OrderId id() {
        return id;
    }

    public CartId cartId() {
        return cartId;
    }

    public UserId userId() {
        return userId;
    }

    public OrderStatus status() {
        return status;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public String currency() {
        return currency;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void touch(ClockProvider clock) {
        this.updatedAt = clock.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order order)) {
            return false;
        }
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
