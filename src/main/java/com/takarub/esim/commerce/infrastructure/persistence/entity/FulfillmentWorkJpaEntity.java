package com.takarub.esim.commerce.infrastructure.persistence.entity;

import java.time.Instant;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;

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
 * JPA persistence representation of {@code FulfillmentWork}. Cross-aggregate Order reference is a
 * scalar id (not {@code @ManyToOne}).
 */
@Entity
@Table(name = "fulfillment_works")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FulfillmentWorkJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false, updatable = false)
    private String orderId;

    @Column(name = "supplier_key", length = 50)
    private String supplierKey;

    @Column(name = "remote_product_id", length = 50)
    private String remoteProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private FulfillmentStatus status;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 255)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FulfillmentWorkJpaEntity(
            String id,
            String orderId,
            String supplierKey,
            String remoteProductId,
            FulfillmentStatus status,
            Instant claimedAt,
            String lastErrorCode,
            String lastErrorMessage,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.supplierKey = supplierKey;
        this.remoteProductId = remoteProductId;
        this.status = status;
        this.claimedAt = claimedAt;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
