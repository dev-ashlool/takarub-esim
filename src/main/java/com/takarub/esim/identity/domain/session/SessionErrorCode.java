package com.takarub.esim.identity.domain.session;

import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Domain error codes for the Session aggregate, implementing the shared {@link ErrorCode} contract.
 */
public enum SessionErrorCode implements ErrorCode {

    INVALID_SESSION_STATE_TRANSITION("SESSION_INVALID_STATE_TRANSITION", "The requested session state transition is not allowed."),
    SESSION_EXPIRED("SESSION_EXPIRED", "The session has expired."),
    SESSION_REVOKED("SESSION_REVOKED", "The session has been revoked."),
    INVALID_REFRESH_TOKEN("SESSION_INVALID_REFRESH_TOKEN", "The supplied refresh token is invalid.");

    private final String code;
    private final String defaultMessage;

    SessionErrorCode(String code, String defaultMessage) {
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
