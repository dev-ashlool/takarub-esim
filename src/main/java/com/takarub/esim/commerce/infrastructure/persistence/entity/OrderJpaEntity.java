package com.takarub.esim.commerce.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.takarub.esim.commerce.domain.order.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA persistence representation of the Order aggregate root. Domain {@code Order} carries no
 * persistence annotations.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "cart_id", length = 36, nullable = false, updatable = false)
    private String cartId;

    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(name = "checkout_request_id", length = 36, nullable = false, updatable = false)
    private String checkoutRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 16, nullable = false)
    private String currency;

    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OrderJpaEntity(
            String id,
            String cartId,
            String userId,
            String checkoutRequestId,
            OrderStatus status,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.cartId = cartId;
        this.userId = userId;
        this.checkoutRequestId = checkoutRequestId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void addItem(OrderItemJpaEntity item) {
        items.add(item);
        item.setOrder(this);
    }
}
