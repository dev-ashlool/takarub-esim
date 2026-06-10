package com.takarub.esim.identity.domain.verification;

import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Domain error codes for the Verification aggregate, implementing the shared {@link ErrorCode}
 * contract.
 */
public enum VerificationErrorCode implements ErrorCode {

    INVALID_VERIFICATION_STATE_TRANSITION("VERIFICATION_INVALID_STATE_TRANSITION", "The requested verification state transition is not allowed."),
    VERIFICATION_EXPIRED("VERIFICATION_EXPIRED", "The verification has expired."),
    VERIFICATION_CONSUMED("VERIFICATION_CONSUMED", "The verification has already been consumed."),
    VERIFICATION_CANCELLED("VERIFICATION_CANCELLED", "The verification has been cancelled.");

    private final String code;
    private final String defaultMessage;

    VerificationErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
