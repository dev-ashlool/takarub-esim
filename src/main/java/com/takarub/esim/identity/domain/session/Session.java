package com.takarub.esim.identity.domain.session;

import java.time.Duration;
import java.time.Instant;

import com.takarub.esim.identity.domain.shared.AggregateRoot;
import com.takarub.esim.identity.domain.session.exception.InvalidRefreshTokenException;
import com.takarub.esim.identity.domain.session.exception.InvalidSessionStateTransitionException;
import com.takarub.esim.identity.domain.session.exception.SessionExpiredException;
import com.takarub.esim.identity.domain.session.exception.SessionRevokedException;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Session aggregate root. Owns the session lifecycle, refresh-token rotation, expiration,
 * revocation and device metadata. References the owning user by {@link UserId} only.
 */
public class Session extends AggregateRoot<SessionId> {

    private final UserId userId;
    private RefreshToken refreshToken;
    private SessionStatus status;
    private DeviceMetadata deviceMetadata;
    private Instant expiresAt;

    private Session(SessionId id, Instant createdAt, UserId userId, RefreshToken refreshToken,
                    SessionStatus status, DeviceMetadata deviceMetadata, Instant expiresAt) {
        super(id, createdAt);
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.status = status;
        this.deviceMetadata = deviceMetadata;
        this.expiresAt = expiresAt;
    }

    public static Session start(IdGenerator idGenerator, ClockProvider clock, UserId userId,
                                DeviceMetadata deviceMetadata, Duration timeToLive) {
        if (userId == null) {
            throw new ValidationException("User id is required to start a session");
        }
        if (deviceMetadata == null) {
            throw new ValidationException("Device metadata is required to start a session");
        }
        requirePositive(timeToLive);
        Instant now = clock.now();
        return new Session(SessionId.generate(idGenerator), now, userId,
                RefreshToken.generate(idGenerator), SessionStatus.ACTIVE, deviceMetadata,
                now.plus(timeToLive));
    }

    private Session(SessionId id, Instant createdAt, Instant updatedAt, UserId userId,
                    RefreshToken refreshToken, SessionStatus status, DeviceMetadata deviceMetadata,
                    Instant expiresAt) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.status = status;
        this.deviceMetadata = deviceMetadata;
        this.expiresAt = expiresAt;
    }

    /**
     * Rebuilds a {@code Session} from already-persisted state. Restores the stored status, refresh
     * token, device metadata and expiry verbatim (including revoked/expired sessions) without
     * running lifecycle rules. For exclusive use by the infrastructure persistence mapper.
     */
    public static Session reconstitute(SessionId id, Instant createdAt, Instant updatedAt,
                                       UserId userId, RefreshToken refreshToken, SessionStatus status,
                                       DeviceMetadata deviceMetadata, Instant expiresAt) {
        return new Session(id, createdAt, updatedAt, userId, refreshToken, status, deviceMetadata,
                expiresAt);
    }

    public RefreshToken rotateRefreshToken(IdGenerator idGenerator, ClockProvider clock, Duration timeToLive) {
        ensureActive(clock);
        requirePositive(timeToLive);
        Instant now = clock.now();
        this.refreshToken = RefreshToken.generate(idGenerator);
        this.expiresAt = now.plus(timeToLive);
        this.deviceMetadata = deviceMetadata.withLastActivityAt(now);
        touch(clock);
        return refreshToken;
    }

    public void verifyRefreshToken(RefreshToken candidate) {
        if (candidate == null || !refreshToken.equals(candidate)) {
            throw new InvalidRefreshTokenException(id());
        }
    }

    public void recordActivity(ClockProvider clock) {
        ensureActive(clock);
        this.deviceMetadata = deviceMetadata.withLastActivityAt(clock.now());
        touch(clock);
    }

    public void revoke(ClockProvider clock) {
        transitionTo(SessionStatus.REVOKED, clock);
    }

    public void expire(ClockProvider clock) {
        transitionTo(SessionStatus.EXPIRED, clock);
    }

    public boolean isExpired(ClockProvider clock) {
        return !clock.now().isBefore(expiresAt);
    }

    private void ensureActive(ClockProvider clock) {
        switch (status) {
            case ACTIVE -> {
                if (isExpired(clock)) {
                    throw new SessionExpiredException(id());
                }
            }
            case REVOKED -> throw new SessionRevokedException(id());
            case EXPIRED -> throw new SessionExpiredException(id());
        }
    }

    private void transitionTo(SessionStatus target, ClockProvider clock) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidSessionStateTransitionException(status, target);
        }
        this.status = target;
        touch(clock);
    }

    private static void requirePositive(Duration timeToLive) {
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new ValidationException("Session time-to-live must be positive");
        }
    }

    public UserId userId() {
        return userId;
    }

    public RefreshToken refreshToken() {
        return refreshToken;
    }

    public SessionStatus status() {
        return status;
    }

    public DeviceMetadata deviceMetadata() {
        return deviceMetadata;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
