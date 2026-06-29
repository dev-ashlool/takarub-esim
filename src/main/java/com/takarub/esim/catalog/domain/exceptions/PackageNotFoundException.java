package com.takarub.esim.catalog.domain.exceptions;

/**
 * Thrown when an internal package lookup yields no matching supplier product.
 */
public final class PackageNotFoundException extends RuntimeException {

    public PackageNotFoundException(String message) {
        super(message);
    }

    public PackageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
