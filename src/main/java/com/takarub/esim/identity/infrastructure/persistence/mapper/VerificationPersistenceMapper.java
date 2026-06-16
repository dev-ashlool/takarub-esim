package com.takarub.esim.identity.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.infrastructure.persistence.entity.VerificationJpaEntity;

/**
 * Translates between the {@link Verification} aggregate and {@link VerificationJpaEntity}. The
 * reverse direction uses the domain's explicit {@link Verification#reconstitute} factory; no
 * reflection is used.
 */
@Component
public class VerificationPersistenceMapper {

    public VerificationJpaEntity toEntity(Verification verification) {
        return new VerificationJpaEntity(
                verification.id().value().toString(),
                verification.userId().value().toString(),
                verification.type(),
                verification.status(),
                verification.expiresAt(),
                verification.createdAt(),
                verification.updatedAt());
    }

    public Verification toDomain(VerificationJpaEntity entity) {
        return Verification.reconstitute(
                VerificationId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                UserId.of(entity.getUserId()),
                entity.getType(),
                entity.getExpiresAt(),
                entity.getStatus());
    }
}
