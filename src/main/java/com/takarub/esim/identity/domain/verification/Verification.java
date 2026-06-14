package com.takarub.esim.identity.domain.verification;

import java.time.Duration;
import java.time.Instant;

import com.takarub.esim.identity.domain.shared.AggregateRoot;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.verification.exception.InvalidVerificationStateTransitionException;
import com.takarub.esim.identity.domain.verification.exception.VerificationCancelledException;
import com.takarub.esim.identity.domain.verification.exception.VerificationConsumedException;
import com.takarub.esim.identity.domain.verification.exception.VerificationExpiredException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Verification aggregate root. Models single-use email-verification and password-reset
 * verifications with an enforced expiration and terminal lifecycle. References the owning user by
 * {@link UserId} only.
 */
public class Verification extends AggregateRoot<VerificationId> {

    private final UserId userId;
    private final VerificationType type;
    private final Instant expiresAt;
    private VerificationStatus status;

    private Verification(VerificationId id, Instant createdAt, UserId userId, VerificationType type,
                         Instant expiresAt, VerificationStatus status) {
        super(id, createdAt);
        this.userId = userId;
        this.type = type;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public static Verification issue(IdGenerator idGenerator, ClockProvider clock,
                                     UserId userId, VerificationType type, Duration timeToLive) {
        if (userId == null) {
            throw new ValidationException("User id is required to issue a verification");
        }
        if (type == null) {
            throw new ValidationException("Verification type is required");
        }
        requirePositive(timeToLive);
        Instant now = clock.now();
        return new Verification(VerificationId.generate(idGenerator), now, userId, type,
                now.plus(timeToLive), VerificationStatus.PENDING);
    }

    private Verification(VerificationId id, Instant createdAt, Instant updatedAt, UserId userId,
                         VerificationType type, Instant expiresAt, VerificationStatus status) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.type = type;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    /**
     * Rebuilds a {@code Verification} from already-persisted state. Restores the stored status
     * verbatim (including terminal states) without running lifecycle rules. For exclusive use by
     * the infrastructure persistence mapper.
     */
    public static Verification reconstitute(VerificationId id, Instant createdAt, Instant updatedAt,
                                            UserId userId, VerificationType type, Instant expiresAt,
                                            VerificationStatus status) {
        return new Verification(id, createdAt, updatedAt, userId, type, expiresAt, status);
    }

    public void consume(ClockProvider clock) {
        ensurePending();
        if (isExpired(clock)) {
            throw new VerificationExpiredException(id());
        }
        transitionTo(VerificationStatus.CONSUMED, clock);
    }

    public void cancel(ClockProvider clock) {
        ensurePending();
        transitionTo(VerificationStatus.CANCELLED, clock);
    }

    public void expire(ClockProvider clock) {
        ensurePending();
        transitionTo(VerificationStatus.EXPIRED, clock);
    }

    public boolean isExpired(ClockProvider clock) {
        return !clock.now().isBefore(expiresAt);
    }

    private void ensurePending() {
        switch (status) {
            case PENDING -> { /* usable */ }
            case CONSUMED -> throw new VerificationConsumedException(id());
            case EXPIRED -> throw new VerificationExpiredException(id());
            case CANCELLED -> throw new VerificationCancelledException(id());
        }
    }

    private void transitionTo(VerificationStatus target, ClockProvider clock) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidVerificationStateTransitionException(status, target);
        }
        this.status = target;
        touch(clock);
    }

    private static void requirePositive(Duration timeToLive) {
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new ValidationException("Verification time-to-live must be positive");
        }
    }

    public UserId userId() {
        return userId;
    }

    public VerificationType type() {
        return type;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public VerificationStatus status() {
        return status;
    }
}
