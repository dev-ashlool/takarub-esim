package com.takarub.esim.identity.domain.user;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.takarub.esim.identity.domain.shared.AggregateRoot;
import com.takarub.esim.identity.domain.user.exception.InvalidUserStateTransitionException;
import com.takarub.esim.identity.domain.user.exception.UserAlreadyActiveException;
import com.takarub.esim.identity.domain.user.exception.UserDeletedException;
import com.takarub.esim.identity.domain.user.exception.UserLockedException;
import com.takarub.esim.identity.domain.user.exception.UserNotVerifiedException;
import com.takarub.esim.identity.domain.user.exception.UserSuspendedException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * User aggregate root. Owns registration, lifecycle / status management and role assignment.
 * State changes happen only through explicit domain methods that enforce the transition rules.
 */
public class User extends AggregateRoot<UserId> {

    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private UserStatus status;
    private final Set<Role> roles;

    private User(UserId id, Instant createdAt, EmailAddress email, PasswordHash passwordHash,
                 UserStatus status, Set<Role> roles) {
        super(id, createdAt);
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.roles = EnumSet.copyOf(roles);
    }

    public static User register(IdGenerator idGenerator, ClockProvider clock,
                                EmailAddress email, PasswordHash passwordHash, Role role) {
        if (email == null) {
            throw new ValidationException("Email is required to register a user");
        }
        if (passwordHash == null) {
            throw new ValidationException("Password hash is required to register a user");
        }
        if (role == null) {
            throw new ValidationException("Initial role is required to register a user");
        }
        return new User(UserId.generate(idGenerator), clock.now(), email, passwordHash,
                UserStatus.PENDING_VERIFICATION, EnumSet.of(role));
    }

    public void verifyEmail(ClockProvider clock) {
        if (status == UserStatus.ACTIVE) {
            throw new UserAlreadyActiveException(id());
        }
        if (status != UserStatus.PENDING_VERIFICATION) {
            throw new InvalidUserStateTransitionException(status, UserStatus.ACTIVE);
        }
        transitionTo(UserStatus.ACTIVE, clock);
    }

    public void lock(ClockProvider clock) {
        transitionTo(UserStatus.LOCKED, clock);
    }

    public void unlock(ClockProvider clock) {
        transitionTo(UserStatus.ACTIVE, clock);
    }

    public void suspend(ClockProvider clock) {
        transitionTo(UserStatus.SUSPENDED, clock);
    }

    public void reinstate(ClockProvider clock) {
        transitionTo(UserStatus.ACTIVE, clock);
    }

    public void delete(ClockProvider clock) {
        transitionTo(UserStatus.DELETED, clock);
    }

    public void assignRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role must not be null");
        }
        roles.add(role);
    }

    public void revokeRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role must not be null");
        }
        roles.remove(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public void ensureCanAuthenticate() {
        switch (status) {
            case ACTIVE -> { /* permitted */ }
            case LOCKED -> throw new UserLockedException(id());
            case SUSPENDED -> throw new UserSuspendedException(id());
            case DELETED -> throw new UserDeletedException(id());
            case PENDING_VERIFICATION -> throw new UserNotVerifiedException(id());
        }
    }

    private void transitionTo(UserStatus target, ClockProvider clock) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidUserStateTransitionException(status, target);
        }
        this.status = target;
        touch(clock);
    }

    public EmailAddress email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }
}
