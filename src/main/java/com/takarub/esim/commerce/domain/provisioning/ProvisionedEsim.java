package com.takarub.esim.commerce.domain.provisioning;

import java.time.Instant;
import java.util.Objects;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Durable customer-usable eSIM provisioning artifact for one fulfilled order (MVP 1:1).
 */
public class ProvisionedEsim {

    private final ProvisionedEsimId id;
    private final OrderId orderId;
    private final FulfillmentId fulfillmentWorkId;
    private final String supplierKey;
    private final String remoteProductId;
    private final String supplierOrderId;
    private final String iccid;
    private final String smdpAddress;
    private final String activationCode;
    private final String pin;
    private final String puk;
    private final String qrString;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProvisionedEsim(
            ProvisionedEsimId id,
            OrderId orderId,
            FulfillmentId fulfillmentWorkId,
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
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.fulfillmentWorkId =
                Objects.requireNonNull(fulfillmentWorkId, "fulfillmentWorkId must not be null");
        this.supplierKey = supplierKey;
        this.remoteProductId = remoteProductId;
        this.supplierOrderId = supplierOrderId;
        this.iccid = iccid;
        this.smdpAddress = smdpAddress;
        this.activationCode = activationCode;
        this.pin = pin;
        this.puk = puk;
        this.qrString = qrString;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ProvisionedEsim create(
            IdGenerator idGenerator,
            ClockProvider clock,
            OrderId orderId,
            FulfillmentId fulfillmentWorkId,
            String supplierKey,
            String remoteProductId,
            String supplierOrderId,
            String iccid,
            String smdpAddress,
            String activationCode,
            String pin,
            String puk,
            String qrString) {
        requireText(supplierKey, "supplierKey");
        requireText(remoteProductId, "remoteProductId");
        requireText(supplierOrderId, "supplierOrderId");
        String normalizedQr = blankToNull(qrString);
        String normalizedSmdp = blankToNull(smdpAddress);
        String normalizedActivation = blankToNull(activationCode);
        requireUsableActivationProof(normalizedQr, normalizedSmdp, normalizedActivation);
        Instant now = clock.now();
        return new ProvisionedEsim(
                ProvisionedEsimId.generate(idGenerator),
                orderId,
                fulfillmentWorkId,
                supplierKey.trim(),
                remoteProductId.trim(),
                supplierOrderId.trim(),
                blankToNull(iccid),
                normalizedSmdp,
                normalizedActivation,
                blankToNull(pin),
                blankToNull(puk),
                normalizedQr,
                now,
                now);
    }

    public static ProvisionedEsim reconstitute(
            ProvisionedEsimId id,
            OrderId orderId,
            FulfillmentId fulfillmentWorkId,
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
        requireText(supplierKey, "supplierKey");
        requireText(remoteProductId, "remoteProductId");
        requireText(supplierOrderId, "supplierOrderId");
        String normalizedQr = blankToNull(qrString);
        String normalizedSmdp = blankToNull(smdpAddress);
        String normalizedActivation = blankToNull(activationCode);
        requireUsableActivationProof(normalizedQr, normalizedSmdp, normalizedActivation);
        return new ProvisionedEsim(
                id,
                orderId,
                fulfillmentWorkId,
                supplierKey.trim(),
                remoteProductId.trim(),
                supplierOrderId.trim(),
                blankToNull(iccid),
                normalizedSmdp,
                normalizedActivation,
                blankToNull(pin),
                blankToNull(puk),
                normalizedQr,
                createdAt,
                updatedAt);
    }

    private static void requireUsableActivationProof(
            String qrString, String smdpAddress, String activationCode) {
        boolean hasQr = qrString != null;
        boolean hasSmdpPair = smdpAddress != null && activationCode != null;
        if (!hasQr && !hasSmdpPair) {
            throw new ValidationException(
                    "Usable activation proof requires qrString or smdpAddress+activationCode");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public ProvisionedEsimId id() {
        return id;
    }

    public OrderId orderId() {
        return orderId;
    }

    public FulfillmentId fulfillmentWorkId() {
        return fulfillmentWorkId;
    }

    public String supplierKey() {
        return supplierKey;
    }

    public String remoteProductId() {
        return remoteProductId;
    }

    public String supplierOrderId() {
        return supplierOrderId;
    }

    public String iccid() {
        return iccid;
    }

    public String smdpAddress() {
        return smdpAddress;
    }

    public String activationCode() {
        return activationCode;
    }

    public String pin() {
        return pin;
    }

    public String puk() {
        return puk;
    }

    public String qrString() {
        return qrString;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProvisionedEsim that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
