package com.takarub.esim.identity.application.result;

import com.takarub.esim.identity.domain.user.UserId;

/**
 * Outcome of confirming a password reset: the user's identity and the number of active sessions
 * revoked as part of the reset (an approved side effect of a successful reset).
 */
public record ConfirmPasswordResetResult(UserId userId, int revokedSessionCount) {
}
