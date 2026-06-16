package com.takarub.esim.identity.domain.session;

import java.util.Optional;

import com.takarub.esim.identity.domain.user.UserId;

/**
 * Repository port for the Session aggregate. Implementations live in the infrastructure layer (not
 * in this task).
 */
public interface SessionRepository {

    Session save(Session session);

    Optional<Session> findById(SessionId sessionId);

    Optional<Session> findActiveSessionByUser(UserId userId);
}
