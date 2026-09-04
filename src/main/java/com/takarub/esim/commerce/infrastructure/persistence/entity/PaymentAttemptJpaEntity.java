package com.takarub.esim.commerce.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA persistence representation of the PaymentAttempt aggregate. Domain {@code PaymentAttempt}
 * carries no persistence annotations. Cross-aggregate Order reference is a scalar id (not
 * {@code @ManyToOne}). The DB-only {@code active_init_order_id} generated column is intentionally
 * not mapped.
 */
@Entity
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttemptJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false, updatable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private PaymentAttemptStatus status;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 16, nullable = false, updatable = false)
    private String currency;

    @Column(name = "external_order_id", length = 255)
    private String externalOrderId;

    @Column(name = "external_transaction_id", length = 255)
    private String externalTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentAttemptJpaEntity(
            String id,
            String orderId,
            PaymentAttemptStatus status,
            BigDecimal amount,
            String currency,
            String externalOrderId,
            String externalTransactionId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.externalOrderId = externalOrderId;
        this.externalTransactionId = externalTransactionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
