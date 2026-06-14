package com.takarub.esim.identity.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.infrastructure.persistence.entity.SessionJpaEntity;

/**
 * Spring Data JPA repository for {@link SessionJpaEntity}.
 */
public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, String> {

    Optional<SessionJpaEntity> findFirstByUserIdAndStatus(String userId, SessionStatus status);
}
