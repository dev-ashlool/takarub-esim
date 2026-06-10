/**
 * Shared exception foundation.
 *
 * <p>Provides the {@link com.takarub.esim.identity.shared.exception.BaseException} hierarchy, the
 * {@link com.takarub.esim.identity.shared.exception.ErrorCode} contract with shared defaults, and
 * the unified {@link com.takarub.esim.identity.shared.exception.ErrorResponse} model.
 *
 * <p>The model is transport-agnostic and designed to be consumed by a {@code GlobalExceptionHandler}
 * in the presentation layer in a later task. No controller is implemented here.
 */
package com.takarub.esim.identity.shared.exception;
