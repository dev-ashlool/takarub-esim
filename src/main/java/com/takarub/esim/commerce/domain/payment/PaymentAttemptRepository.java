package com.takarub.esim.commerce.domain.payment;

import java.util.List;
import java.util.Optional;

import com.takarub.esim.commerce.domain.order.OrderId;

/**
 * Domain repository port for the {@link PaymentAttempt} aggregate. Implementations live in
 * infrastructure.
 *
 * <p>One Order may have many payment attempts. Callers that start payment must uphold: at most one
 * {@link PaymentAttemptStatus#INITIATED} attempt per Order. That cross-aggregate uniqueness is
 * enforced by application orchestration and persistence constraints, not by this aggregate alone.
 */
public interface PaymentAttemptRepository {

    PaymentAttempt save(PaymentAttempt paymentAttempt);

    Optional<PaymentAttempt> findById(PaymentAttemptId id);

    /**
     * Returns all payment attempts for the given order (history / retries). Does not imply
     * one-attempt-per-order cardinality.
     */
    List<PaymentAttempt> findByOrderId(OrderId orderId);

    /**
     * Returns the active {@link PaymentAttemptStatus#INITIATED} attempt for the order, if any.
     * Used by future StartPayment to reuse an in-progress attempt.
     */
    Optional<PaymentAttempt> findActiveInitiatedByOrderId(OrderId orderId);
}
