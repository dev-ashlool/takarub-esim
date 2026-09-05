package com.takarub.esim.commerce.domain.fulfillment;

import java.time.Instant;
import java.util.Objects;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Durable fulfillment work for a paid order. Created when payment is confirmed; supplier HTTP is
 * never performed by this aggregate.
 */
public class FulfillmentWork {

    private final FulfillmentId id;
    private final OrderId orderId;
    private final String supplierKey;
    private final String remoteProductId;
    private final FulfillmentStatus status;
    private final Instant claimedAt;
    private final String lastErrorCode;
    private final String lastErrorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    private FulfillmentWork(
            FulfillmentId id,
            OrderId orderId,
            String supplierKey,
            String remoteProductId,
            FulfillmentStatus status,
            Instant claimedAt,
            String lastErrorCode,
            String lastErrorMessage,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.supplierKey = supplierKey;
        this.remoteProductId = remoteProductId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.claimedAt = claimedAt;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Creates PENDING work with frozen supplier sourcing required for a later purchase attempt.
     */
    public static FulfillmentWork pending(
            IdGenerator idGenerator,
            ClockProvider clock,
            OrderId orderId,
            String supplierKey,
            String remoteProductId) {
        requireText(supplierKey, "supplierKey");
        requireText(remoteProductId, "remoteProductId");
        Instant now = clock.now();
        return new FulfillmentWork(
                FulfillmentId.generate(idGenerator),
                orderId,
                supplierKey.trim(),
                remoteProductId.trim(),
                FulfillmentStatus.PENDING,
                null,
                null,
                null,
                now,
                now);
    }

    /**
     * Creates BLOCKED work when local/data conditions prevent a safe supplier attempt.
     */
    public static FulfillmentWork blocked(
            IdGenerator idGenerator,
            ClockProvider clock,
            OrderId orderId,
            String supplierKey,
            String remoteProductId,
            String lastErrorCode,
            String lastErrorMessage) {
        requireText(lastErrorCode, "lastErrorCode");
        requireText(lastErrorMessage, "lastErrorMessage");
        Instant now = clock.now();
        return new FulfillmentWork(
                FulfillmentId.generate(idGenerator),
                orderId,
                blankToNull(supplierKey),
                blankToNull(remoteProductId),
                FulfillmentStatus.BLOCKED,
                null,
                lastErrorCode.trim(),
                lastErrorMessage.trim(),
                now,
                now);
    }

    /**
     * Rebuilds work from persisted state without create orchestration. For exclusive use by the
     * infrastructure persistence mapper.
     */
    public static FulfillmentWork reconstitute(
            FulfillmentId id,
            OrderId orderId,
            String supplierKey,
            String remoteProductId,
            FulfillmentStatus status,
            Instant claimedAt,
            String lastErrorCode,
            String lastErrorMessage,
            Instant createdAt,
            Instant updatedAt) {
        if (status == null) {
            throw new ValidationException("status is required");
        }
        switch (status) {
            case PENDING -> {
                requireText(supplierKey, "supplierKey");
                requireText(remoteProductId, "remoteProductId");
                requireNull(claimedAt, "claimedAt");
                requireAbsent(lastErrorCode, "lastErrorCode");
                requireAbsent(lastErrorMessage, "lastErrorMessage");
            }
            case PROCESSING -> {
                requireText(supplierKey, "supplierKey");
                requireText(remoteProductId, "remoteProductId");
                requirePresent(claimedAt, "claimedAt");
            }
            case FULFILLED -> {
                requireText(supplierKey, "supplierKey");
                requireText(remoteProductId, "remoteProductId");
            }
            case UNKNOWN -> {
                requireText(supplierKey, "supplierKey");
                requireText(remoteProductId, "remoteProductId");
                requirePresent(claimedAt, "claimedAt");
                requireText(lastErrorCode, "lastErrorCode");
                requireText(lastErrorMessage, "lastErrorMessage");
            }
            case BLOCKED -> {
                requireText(lastErrorCode, "lastErrorCode");
                requireText(lastErrorMessage, "lastErrorMessage");
            }
            default -> throw new ValidationException("Unsupported fulfillment status: " + status);
        }
        return new FulfillmentWork(
                id,
                orderId,
                blankToNull(supplierKey),
                blankToNull(remoteProductId),
                status,
                claimedAt,
                blankToNull(lastErrorCode),
                blankToNull(lastErrorMessage),
                createdAt,
                updatedAt);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    private static void requireAbsent(String value, String field) {
        if (value != null && !value.isBlank()) {
            throw new ValidationException(field + " must be null");
        }
    }

    private static void requireNull(Instant value, String field) {
        if (value != null) {
            throw new ValidationException(field + " must be null");
        }
    }

    private static void requirePresent(Instant value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public FulfillmentId id() {
        return id;
    }

    public OrderId orderId() {
        return orderId;
    }

    public String supplierKey() {
        return supplierKey;
    }

    public String remoteProductId() {
        return remoteProductId;
    }

    public FulfillmentStatus status() {
        return status;
    }

    public Instant claimedAt() {
        return claimedAt;
    }

    public String lastErrorCode() {
        return lastErrorCode;
    }

    public String lastErrorMessage() {
        return lastErrorMessage;
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
        if (!(o instanceof FulfillmentWork that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
