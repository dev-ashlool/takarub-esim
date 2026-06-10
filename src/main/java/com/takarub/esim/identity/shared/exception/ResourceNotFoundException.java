package com.takarub.esim.identity.shared.exception;

/**
 * Signals that a requested resource could not be found.
 *
 * <p>Defaults to {@link SharedErrorCode#RESOURCE_NOT_FOUND}.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(SharedErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
