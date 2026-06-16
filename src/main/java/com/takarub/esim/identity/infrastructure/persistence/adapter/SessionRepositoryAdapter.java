package com.takarub.esim.identity.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.persistence.mapper.SessionPersistenceMapper;
import com.takarub.esim.identity.infrastructure.persistence.repository.SessionJpaRepository;

/**
 * Outbound adapter implementing the {@link SessionRepository} domain port over Spring Data JPA.
 * "Active" session lookup is expressed as a status filter on the underlying query.
 */
@Component
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository sessionJpaRepository;
    private final SessionPersistenceMapper mapper;

    public SessionRepositoryAdapter(SessionJpaRepository sessionJpaRepository,
                                    SessionPersistenceMapper mapper) {
        this.sessionJpaRepository = sessionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Session save(Session session) {
        return mapper.toDomain(sessionJpaRepository.save(mapper.toEntity(session)));
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        return sessionJpaRepository.findById(sessionId.value().toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<Session> findActiveSessionByUser(UserId userId) {
        return sessionJpaRepository
                .findFirstByUserIdAndStatus(userId.value().toString(), SessionStatus.ACTIVE)
                .map(mapper::toDomain);
    }
}
