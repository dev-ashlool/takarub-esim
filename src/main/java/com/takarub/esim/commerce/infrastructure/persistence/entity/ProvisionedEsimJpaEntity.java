package com.takarub.esim.commerce.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA persistence representation of provisioned eSIM. Cross-aggregate refs are scalar ids.
 */
@Entity
@Table(name = "provisioned_esims")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProvisionedEsimJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false, updatable = false)
    private String orderId;

    @Column(name = "fulfillment_work_id", length = 36, nullable = false, updatable = false)
    private String fulfillmentWorkId;

    @Column(name = "supplier_key", length = 50, nullable = false, updatable = false)
    private String supplierKey;

    @Column(name = "remote_product_id", length = 50, nullable = false, updatable = false)
    private String remoteProductId;

    @Column(name = "supplier_order_id", length = 255, nullable = false)
    private String supplierOrderId;

    @Column(name = "iccid", length = 32)
    private String iccid;

    @Column(name = "smdp_address", length = 255)
    private String smdpAddress;

    @Column(name = "activation_code", length = 255)
    private String activationCode;

    @Column(name = "pin", length = 32)
    private String pin;

    @Column(name = "puk", length = 32)
    private String puk;

    @Lob
    @Column(name = "qr_string")
    private String qrString;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProvisionedEsimJpaEntity(
            String id,
            String orderId,
            String fulfillmentWorkId,
            String supplierKey,
            String remoteProductId,
            String supplierOrderId,
            String iccid,
            String smdpAddress,
            String activationCode,
            String pin,
            String puk,
            String qrString,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.fulfillmentWorkId = fulfillmentWorkId;
        this.supplierKey = supplierKey;
        this.remoteProductId = remoteProductId;
        this.supplierOrderId = supplierOrderId;
        this.iccid = iccid;
        this.smdpAddress = smdpAddress;
        this.activationCode = activationCode;
        this.pin = pin;
        this.puk = puk;
        this.qrString = qrString;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
