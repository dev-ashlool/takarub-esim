package com.takarub.esim.commerce.domain.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * PaymentAttempt aggregate root. Models one payment attempt for a single Order. Does not load
 * Order, Cart, or payment providers; the application supplies amount/currency snapshots and drives
 * Order transitions separately.
 */
public class PaymentAttempt {

    private static final int MONEY_SCALE = 2;

    private final PaymentAttemptId id;
    private final OrderId orderId;
    private PaymentAttemptStatus status;
    private final BigDecimal amount;
    private final String currency;
    private String externalOrderId;
    private String externalTransactionId;
    private final Instant createdAt;
    private Instant updatedAt;

    private PaymentAttempt(PaymentAttemptId id, Instant createdAt, Instant updatedAt, OrderId orderId,
                           PaymentAttemptStatus status, BigDecimal amount, String currency,
                           String externalOrderId, String externalTransactionId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.externalOrderId = externalOrderId;
        this.externalTransactionId = externalTransactionId;
    }

    /**
     * Creates a new {@link PaymentAttemptStatus#INITIATED} payment attempt with no external
     * correlation identifiers.
     */
    public static PaymentAttempt create(IdGenerator idGenerator, ClockProvider clock, OrderId orderId,
                                        BigDecimal amount, String currency) {
        if (orderId == null) {
            throw new ValidationException("Order id is required to create a payment attempt");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be positive");
        }
        requireText(currency, "currency");

        Instant now = clock.now();
        return new PaymentAttempt(
                PaymentAttemptId.generate(idGenerator),
                now,
                now,
                orderId,
                PaymentAttemptStatus.INITIATED,
                scaleMoney(amount),
                currency.trim(),
                null,
                null);
    }

    /**
     * Rebuilds a payment attempt from persisted state without running create rules. For exclusive
     * use by the infrastructure persistence mapper.
     */
    public static PaymentAttempt reconstitute(PaymentAttemptId id, Instant createdAt, Instant updatedAt,
                                              OrderId orderId, PaymentAttemptStatus status,
                                              BigDecimal amount, String currency,
                                              String externalOrderId, String externalTransactionId) {
        return new PaymentAttempt(
                id,
                createdAt,
                updatedAt,
                orderId,
                status,
                amount,
                currency,
                externalOrderId,
                externalTransactionId);
    }

    /**
     * Binds a provider-neutral external order correlation id while {@link PaymentAttemptStatus#INITIATED}.
     * First assignment updates {@code updatedAt}; same normalized value is an idempotent no-op;
     * a different value conflicts.
     */
    public void assignExternalOrderId(ClockProvider clock, String externalOrderId) {
        ensureInitiatedForExternalAssignment();
        String normalized = normalizeExternalId(externalOrderId, "externalOrderId");
        if (this.externalOrderId == null) {
            this.externalOrderId = normalized;
            touch(clock);
            return;
        }
        if (this.externalOrderId.equals(normalized)) {
            return;
        }
        throw new ConflictException("externalOrderId is already assigned and cannot be changed");
    }

    /**
     * Binds a provider-neutral external transaction correlation id while
     * {@link PaymentAttemptStatus#INITIATED}. First assignment updates {@code updatedAt}; same
     * normalized value is an idempotent no-op; a different value conflicts.
     */
    public void assignExternalTransactionId(ClockProvider clock, String externalTransactionId) {
        ensureInitiatedForExternalAssignment();
        String normalized = normalizeExternalId(externalTransactionId, "externalTransactionId");
        if (this.externalTransactionId == null) {
            this.externalTransactionId = normalized;
            touch(clock);
            return;
        }
        if (this.externalTransactionId.equals(normalized)) {
            return;
        }
        throw new ConflictException("externalTransactionId is already assigned and cannot be changed");
    }

    /**
     * Transitions {@link PaymentAttemptStatus#INITIATED} to {@link PaymentAttemptStatus#CONFIRMED}.
     */
    public void confirm(ClockProvider clock) {
        if (status == PaymentAttemptStatus.CONFIRMED) {
            throw new ConflictException("Payment attempt is already confirmed");
        }
        if (status == PaymentAttemptStatus.FAILED) {
            throw new ConflictException("Failed payment attempt cannot be confirmed");
        }
        if (status != PaymentAttemptStatus.INITIATED) {
            throw new ConflictException("Invalid payment attempt state for confirm: " + status);
        }
        this.status = PaymentAttemptStatus.CONFIRMED;
        touch(clock);
    }

    /**
     * Transitions {@link PaymentAttemptStatus#INITIATED} to {@link PaymentAttemptStatus#FAILED}.
     */
    public void fail(ClockProvider clock) {
        if (status == PaymentAttemptStatus.FAILED) {
            throw new ConflictException("Payment attempt is already failed");
        }
        if (status == PaymentAttemptStatus.CONFIRMED) {
            throw new ConflictException("Confirmed payment attempt cannot be failed");
        }
        if (status != PaymentAttemptStatus.INITIATED) {
            throw new ConflictException("Invalid payment attempt state for fail: " + status);
        }
        this.status = PaymentAttemptStatus.FAILED;
        touch(clock);
    }

    public PaymentAttemptId id() {
        return id;
    }

    public OrderId orderId() {
        return orderId;
    }

    public PaymentAttemptStatus status() {
        return status;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public String externalOrderId() {
        return externalOrderId;
    }

    public String externalTransactionId() {
        return externalTransactionId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void ensureInitiatedForExternalAssignment() {
        if (status != PaymentAttemptStatus.INITIATED) {
            throw new ConflictException(
                    "External identifiers can only be assigned while payment attempt is initiated");
        }
    }

    private static String normalizeExternalId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(field + " is required");
        }
        return trimmed;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    private static BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void touch(ClockProvider clock) {
        this.updatedAt = clock.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentAttempt that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
