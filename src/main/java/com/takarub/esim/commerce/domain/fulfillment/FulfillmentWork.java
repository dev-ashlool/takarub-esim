package com.takarub.esim.commerce.domain.fulfillment;

import java.time.Instant;
import java.util.Objects;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Durable fulfillment work for a paid order. Created when payment is confirmed; supplier HTTP is
 * never performed by this aggregate.
 */
public class FulfillmentWork {

    /**
     * Error code persisted when stale {@link FulfillmentStatus#PROCESSING} is reconciled to
     * {@link FulfillmentStatus#UNKNOWN}. Does not imply a safe repurchase.
     */
    public static final String STALE_PROCESSING_ERROR_CODE = "FULFILLMENT_STALE_PROCESSING";

    /**
     * Safe operator-facing message for stale PROCESSING reconciliation. Contains no supplier or
     * activation secrets.
     */
    public static final String STALE_PROCESSING_ERROR_MESSAGE =
            "Fulfillment execution exceeded the processing threshold and requires reconciliation.";

    private final FulfillmentId id;
    private final OrderId orderId;
    private final String supplierKey;
    private final String remoteProductId;
    private FulfillmentStatus status;
    private final Instant claimedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private final Instant createdAt;
    private Instant updatedAt;

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
                requirePresent(claimedAt, "claimedAt");
                requireAbsent(lastErrorCode, "lastErrorCode");
                requireAbsent(lastErrorMessage, "lastErrorMessage");
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

    /**
     * Transitions {@link FulfillmentStatus#PROCESSING} to {@link FulfillmentStatus#FULFILLED}.
     */
    public void markFulfilled(ClockProvider clock) {
        requireProcessing("markFulfilled");
        requirePresent(claimedAt, "claimedAt");
        this.status = FulfillmentStatus.FULFILLED;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.updatedAt = clock.now();
    }

    /**
     * Transitions {@link FulfillmentStatus#PROCESSING} to {@link FulfillmentStatus#UNKNOWN}.
     */
    public void markUnknown(ClockProvider clock, String errorCode, String errorMessage) {
        requireProcessing("markUnknown");
        requireText(errorCode, "lastErrorCode");
        requireText(errorMessage, "lastErrorMessage");
        this.status = FulfillmentStatus.UNKNOWN;
        this.lastErrorCode = errorCode.trim();
        this.lastErrorMessage = errorMessage.trim();
        this.updatedAt = clock.now();
    }

    /**
     * Marks stale {@link FulfillmentStatus#PROCESSING} as {@link FulfillmentStatus#UNKNOWN} for
     * operator reconciliation. Does not authorize a supplier repurchase.
     */
    public void markUnknownFromStaleProcessing(ClockProvider clock) {
        markUnknown(clock, STALE_PROCESSING_ERROR_CODE, STALE_PROCESSING_ERROR_MESSAGE);
    }

    /**
     * Transitions {@link FulfillmentStatus#PROCESSING} to {@link FulfillmentStatus#BLOCKED}.
     */
    public void markBlocked(ClockProvider clock, String errorCode, String errorMessage) {
        requireProcessing("markBlocked");
        requireText(errorCode, "lastErrorCode");
        requireText(errorMessage, "lastErrorMessage");
        this.status = FulfillmentStatus.BLOCKED;
        this.lastErrorCode = errorCode.trim();
        this.lastErrorMessage = errorMessage.trim();
        this.updatedAt = clock.now();
    }

    private void requireProcessing(String action) {
        if (status != FulfillmentStatus.PROCESSING) {
            throw new ConflictException(
                    "Fulfillment must be PROCESSING to " + action + "; current status: " + status);
        }
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
