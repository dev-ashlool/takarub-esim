package com.takarub.esim.identity.domain.user;

import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Domain error codes for the User aggregate, implementing the shared {@link ErrorCode} contract.
 */
public enum UserErrorCode implements ErrorCode {

    INVALID_USER_STATE_TRANSITION("USER_INVALID_STATE_TRANSITION", "The requested user state transition is not allowed."),
    USER_ALREADY_ACTIVE("USER_ALREADY_ACTIVE", "The user is already active."),
    USER_NOT_VERIFIED("USER_NOT_VERIFIED", "The user has not verified their email."),
    USER_LOCKED("USER_LOCKED", "The user account is locked."),
    USER_SUSPENDED("USER_SUSPENDED", "The user account is suspended."),
    USER_DELETED("USER_DELETED", "The user account is deleted.");

    private final String code;
    private final String defaultMessage;

    UserErrorCode(String code, String defaultMessage) {
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
