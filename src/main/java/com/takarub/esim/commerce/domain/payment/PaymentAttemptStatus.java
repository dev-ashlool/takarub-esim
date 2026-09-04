package com.takarub.esim.commerce.domain.payment;

/**
 * Lifecycle of a {@link PaymentAttempt}. {@link #CONFIRMED} and {@link #FAILED} are terminal.
 *
 * <p>{@link #INITIATED} means Takarub opened a payment attempt; it does not imply a provider
 * session exists or that money moved.
 */
public enum PaymentAttemptStatus {
    INITIATED,
    CONFIRMED,
    FAILED
}
