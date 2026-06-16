package com.takarub.esim.identity.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.domain.verification.VerificationRepository;
import com.takarub.esim.identity.domain.verification.VerificationStatus;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.infrastructure.persistence.mapper.VerificationPersistenceMapper;
import com.takarub.esim.identity.infrastructure.persistence.repository.VerificationJpaRepository;

/**
 * Outbound adapter implementing the {@link VerificationRepository} domain port over Spring Data
 * JPA. "Active" verification lookup is expressed as a PENDING status filter on the query.
 */
@Component
public class VerificationRepositoryAdapter implements VerificationRepository {

    private final VerificationJpaRepository verificationJpaRepository;
    private final VerificationPersistenceMapper mapper;

    public VerificationRepositoryAdapter(VerificationJpaRepository verificationJpaRepository,
                                         VerificationPersistenceMapper mapper) {
        this.verificationJpaRepository = verificationJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Verification save(Verification verification) {
        return mapper.toDomain(verificationJpaRepository.save(mapper.toEntity(verification)));
    }

    @Override
    public Optional<Verification> findById(VerificationId verificationId) {
        return verificationJpaRepository.findById(verificationId.value().toString())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Verification> findActiveVerification(UserId userId, VerificationType type) {
        return verificationJpaRepository
                .findFirstByUserIdAndTypeAndStatus(
                        userId.value().toString(), type, VerificationStatus.PENDING)
                .map(mapper::toDomain);
    }
}
