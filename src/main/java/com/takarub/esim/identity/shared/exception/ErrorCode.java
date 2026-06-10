package com.takarub.esim.identity.shared.exception;

/**
 * Stable, machine-readable error code contract.
 *
 * <p>Each module or layer may define its own {@code enum} implementing this interface; the shared
 * cross-cutting defaults live in {@link SharedErrorCode}.
 *
 * <p>Intentionally transport-agnostic: HTTP status mapping is the responsibility of the future
 * {@code GlobalExceptionHandler}, not of the error code itself.
 */
public interface ErrorCode {

    /**
     * @return the stable, unique code string (e.g. {@code "RESOURCE_NOT_FOUND"})
     */
    String code();

    /**
     * @return a human-readable default message for this code
     */
    String defaultMessage();
}
