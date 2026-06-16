package com.takarub.esim.identity.application.result;

import java.time.Instant;
import java.util.Set;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;

/**
 * Immutable read model projecting a {@link User} for query responses. Carries no behaviour and no
 * sensitive credential data (the password hash is never exposed).
 */
public record UserView(
        UserId id,
        EmailAddress email,
        UserStatus status,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserView from(User user) {
        return new UserView(
                user.id(),
                user.email(),
                user.status(),
                user.roles(),
                user.createdAt(),
                user.updatedAt());
    }
}
