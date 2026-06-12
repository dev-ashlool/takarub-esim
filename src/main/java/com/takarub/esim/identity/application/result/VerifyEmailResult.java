package com.takarub.esim.identity.application.result;

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;

/**
 * Outcome of verifying a user's e-mail: the user's identity and resulting status.
 */
public record VerifyEmailResult(UserId userId, UserStatus status) {
}
