package com.takarub.esim.identity.domain.session;

/**
 * Lifecycle status of a {@link Session}. Once a session leaves {@code ACTIVE} it is terminal.
 */
public enum SessionStatus {

    ACTIVE,
    REVOKED,
    EXPIRED;

    public boolean canTransitionTo(SessionStatus target) {
        return switch (this) {
            case ACTIVE -> target == REVOKED || target == EXPIRED;
            case REVOKED, EXPIRED -> false;
        };
    }
}
