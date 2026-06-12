package com.takarub.esim.identity.application.result;

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.verification.VerificationId;

/**
 * Outcome of requesting a password reset: the target user's identity and the issued password-reset
 * verification's identifier. The opaque token is delivered out-of-band and is not returned here.
 */
public record RequestPasswordResetResult(UserId userId, VerificationId verificationId) {
}
