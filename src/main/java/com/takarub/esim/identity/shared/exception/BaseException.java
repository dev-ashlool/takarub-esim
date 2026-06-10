package com.takarub.esim.identity.shared.exception;

import java.util.Objects;

/**
 * Root of the shared exception hierarchy.
 *
 * <p>Every shared exception carries a stable {@link ErrorCode} so that the future
 * {@code GlobalExceptionHandler} can translate any exception into a unified {@link ErrorResponse}
 * without inspecting concrete types. Unchecked by design (extends {@link RuntimeException}).
 */
public abstract class BaseException extends RuntimeException {

    /** {@link ErrorCode} is not guaranteed serializable; excluded from serialization. */
    private final transient ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    protected BaseException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
