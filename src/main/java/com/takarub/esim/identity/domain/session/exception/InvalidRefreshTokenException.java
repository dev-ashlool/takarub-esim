package com.takarub.esim.identity.domain.session.exception;

import com.takarub.esim.identity.domain.session.SessionErrorCode;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a supplied refresh token does not match the session's current token.
 */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException(SessionId sessionId) {
        super(SessionErrorCode.INVALID_REFRESH_TOKEN,
                "Invalid refresh token for session " + sessionId.value() + ".");
    }
}
