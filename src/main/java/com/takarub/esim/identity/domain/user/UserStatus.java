package com.takarub.esim.identity.domain.user;

/**
 * Lifecycle status of a {@link User}. The transition table is the single source of truth for
 * which status changes are permitted.
 */
public enum UserStatus {

    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    SUSPENDED,
    DELETED;

    public boolean canTransitionTo(UserStatus target) {
        return switch (this) {
            case PENDING_VERIFICATION -> target == ACTIVE;
            case ACTIVE -> target == LOCKED || target == SUSPENDED || target == DELETED;
            case LOCKED -> target == ACTIVE || target == DELETED;
            case SUSPENDED -> target == ACTIVE || target == DELETED;
            case DELETED -> false;
        };
    }
}
