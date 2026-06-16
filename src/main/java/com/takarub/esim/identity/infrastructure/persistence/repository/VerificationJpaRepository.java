package com.takarub.esim.identity.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.identity.domain.verification.VerificationStatus;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.infrastructure.persistence.entity.VerificationJpaEntity;

/**
 * Spring Data JPA repository for {@link VerificationJpaEntity}.
 */
public interface VerificationJpaRepository extends JpaRepository<VerificationJpaEntity, String> {

    Optional<VerificationJpaEntity> findFirstByUserIdAndTypeAndStatus(
            String userId, VerificationType type, VerificationStatus status);
}
