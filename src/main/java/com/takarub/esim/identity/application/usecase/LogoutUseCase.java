package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.command.LogoutCommand;
import com.takarub.esim.identity.application.command.RevokeSessionCommand;
import com.takarub.esim.identity.application.result.LogoutResult;
import com.takarub.esim.identity.application.result.RevokeSessionResult;

/**
 * Logs out the current session by revoking it.
 *
 * <p>Delegates to {@link RevokeSessionUseCase}; no duplicate revocation logic.
 */
public class LogoutUseCase {

    private final RevokeSessionUseCase revokeSessionUseCase;

    public LogoutUseCase(RevokeSessionUseCase revokeSessionUseCase) {
        this.revokeSessionUseCase = revokeSessionUseCase;
    }

    public LogoutResult execute(LogoutCommand command) {
        RevokeSessionResult revoked = revokeSessionUseCase.execute(
                new RevokeSessionCommand(command.sessionId()));
        return new LogoutResult(revoked.sessionId(), revoked.status());
    }
}
