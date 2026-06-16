package com.takarub.esim.identity.application.exception;

import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Application-layer error codes for the Identity module, implementing the shared {@link ErrorCode}
 * contract. These are orchestration-level codes (lookup failures, cross-aggregate conflicts) and
 * are kept separate from domain error codes.
 */
public enum IdentityApplicationErrorCode implements ErrorCode {

    USER_NOT_FOUND("IDENTITY_USER_NOT_FOUND", "The requested user was not found."),
    VERIFICATION_NOT_FOUND("IDENTITY_VERIFICATION_NOT_FOUND", "The requested verification was not found."),
    SESSION_NOT_FOUND("IDENTITY_SESSION_NOT_FOUND", "The requested session was not found."),
    DUPLICATE_USER("IDENTITY_DUPLICATE_USER", "A user with the supplied e-mail already exists.");

    private final String code;
    private final String defaultMessage;

    IdentityApplicationErrorCode(String code, String defaultMessage) {
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
