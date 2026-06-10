package com.takarub.esim.identity.domain.verification;

/**
 * Lifecycle status of a {@link Verification}. A verification is single-use: once it leaves
 * {@code PENDING} it is terminal.
 */
public enum VerificationStatus {

    PENDING,
    CONSUMED,
    EXPIRED,
    CANCELLED;

    public boolean canTransitionTo(VerificationStatus target) {
        return switch (this) {
            case PENDING -> target == CONSUMED || target == EXPIRED || target == CANCELLED;
            case CONSUMED, EXPIRED, CANCELLED -> false;
        };
    }
}
