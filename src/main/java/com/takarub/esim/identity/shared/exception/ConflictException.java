package com.takarub.esim.identity.shared.exception;

/**
 * Signals that the request conflicts with the current state of the resource.
 *
 * <p>Defaults to {@link SharedErrorCode#CONFLICT}.
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(SharedErrorCode.CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
