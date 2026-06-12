package com.takarub.esim.identity.application.usecase;

import java.time.Duration;
import java.util.Optional;

import com.takarub.esim.identity.application.command.CreateSessionCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.result.CreateSessionResult;
import com.takarub.esim.identity.domain.session.DeviceMetadata;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Creates a session for an authentication-eligible user, enforcing one active session per user.
 *
 * <p>Workflow: load the user, validate authentication eligibility, revoke any existing active
 * session, start a new session and persist it. Owns the transaction boundary for the create-session
 * workflow.
 */
public class CreateSessionUseCase {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final Duration sessionTtl;

    public CreateSessionUseCase(UserRepository userRepository,
                                SessionRepository sessionRepository,
                                IdGenerator idGenerator,
                                ClockProvider clock,
                                Duration sessionTtl) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    public CreateSessionResult execute(CreateSessionCommand command) {
        UserId userId = UserId.of(command.userId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundApplicationException(userId));
        user.ensureCanAuthenticate();

        Optional<Session> existingActive = sessionRepository.findActiveSessionByUser(userId);
        if (existingActive.isPresent()) {
            Session current = existingActive.get();
            current.revoke(clock);
            sessionRepository.save(current);
        }

        DeviceMetadata deviceMetadata = DeviceMetadata.of(
                command.deviceName(), command.deviceType(), command.ipAddress(),
                command.userAgent(), clock.now());
        Session session = Session.start(idGenerator, clock, userId, deviceMetadata, sessionTtl);
        sessionRepository.save(session);

        return new CreateSessionResult(
                session.id(), userId, session.refreshToken(), session.expiresAt());
    }
}
