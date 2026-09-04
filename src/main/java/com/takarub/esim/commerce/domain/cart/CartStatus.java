package com.takarub.esim.commerce.domain.cart;

/**
 * Lifecycle of a {@link Cart}. Only {@link #OPEN} carts may be modified.
 * {@link #CHECKED_OUT} and {@link #CANCELED} are terminal for current cart behavior.
 */
public enum CartStatus {
    OPEN,
    CHECKED_OUT,
    CANCELED
}
