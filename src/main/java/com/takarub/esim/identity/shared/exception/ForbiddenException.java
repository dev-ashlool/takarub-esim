package com.takarub.esim.identity.shared.exception;

/**
 * Signals that access to the requested resource is forbidden.
 *
 * <p>Defaults to {@link SharedErrorCode#FORBIDDEN}.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(SharedErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
