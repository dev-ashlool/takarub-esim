package com.takarub.esim.identity.shared.exception;

/**
 * A single field-level violation, suitable for inclusion in {@link ErrorResponse#errors()}.
 *
 * @param field   the offending field / property name
 * @param message the human-readable reason the value was rejected
 */
public record FieldViolation(String field, String message) {
}
