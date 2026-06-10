package com.takarub.esim.identity.shared.exception;

/**
 * Signals that authentication is required or has failed.
 *
 * <p>Defaults to {@link SharedErrorCode#UNAUTHORIZED}.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(SharedErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
