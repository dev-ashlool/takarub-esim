package com.takarub.esim.identity.shared.exception;

/**
 * Signals a violation of a business rule or invariant.
 *
 * <p>Defaults to {@link SharedErrorCode#BUSINESS_RULE_VIOLATION}; callers may supply a more
 * specific {@link ErrorCode}.
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(SharedErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
