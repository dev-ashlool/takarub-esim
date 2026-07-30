package com.takarub.esim.commerce.domain.cart;

/**
 * Lifecycle of a {@link Cart}. Only {@link #OPEN} carts may be modified.
 */
public enum CartStatus {
    OPEN,
    CHECKED_OUT
}
