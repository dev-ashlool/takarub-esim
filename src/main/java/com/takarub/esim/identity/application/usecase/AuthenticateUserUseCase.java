package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.command.AuthenticateUserCommand;
import com.takarub.esim.identity.application.command.AuthenticateUserDeviceMetadata;
import com.takarub.esim.identity.application.command.CreateSessionCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.port.AccessTokenIssuer;
import com.takarub.esim.identity.application.port.PasswordHasher;
import com.takarub.esim.identity.application.result.AuthenticateUserResult;
import com.takarub.esim.identity.application.result.CreateSessionResult;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;

/**
 * Authenticates a user with e-mail and password, enforces eligibility, creates a session and issues
 * an access token via the {@link AccessTokenIssuer} port.
 */
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final CreateSessionUseCase createSessionUseCase;
    private final AccessTokenIssuer accessTokenIssuer;

    public AuthenticateUserUseCase(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   CreateSessionUseCase createSessionUseCase,
                                   AccessTokenIssuer accessTokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.createSessionUseCase = createSessionUseCase;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public AuthenticateUserResult execute(AuthenticateUserCommand command) {
        EmailAddress email = EmailAddress.of(command.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundApplicationException(email));

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new UnauthorizedException("Invalid e-mail or password.");
        }

        user.ensureCanAuthenticate();

        AuthenticateUserDeviceMetadata device = command.deviceMetadata();
        CreateSessionResult session = createSessionUseCase.execute(new CreateSessionCommand(
                user.id().value().toString(),
                device.deviceName(),
                device.deviceType(),
                device.ipAddress(),
                device.userAgent()));

        String accessToken = accessTokenIssuer.issueAccessToken(
                user.id(), session.sessionId(), user.roles(), user.email());

        return new AuthenticateUserResult(
                user.id(),
                session.sessionId(),
                accessToken,
                session.refreshToken(),
                session.expiresAt());
    }
}
