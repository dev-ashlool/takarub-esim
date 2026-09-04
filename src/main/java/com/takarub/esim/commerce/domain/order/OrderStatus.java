package com.takarub.esim.commerce.domain.order;

/**
 * Lifecycle of an {@link Order}. {@link #PAID} is terminal for the current commerce payment flow.
 */
public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    PAID
}
