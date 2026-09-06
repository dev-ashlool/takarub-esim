package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.FulfillmentWorkPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.FulfillmentWorkJpaRepository;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Outbound adapter implementing {@link FulfillmentWorkRepository} over Spring Data JPA.
 */
@Component
public class FulfillmentWorkRepositoryAdapter implements FulfillmentWorkRepository {

    static final int CLAIM_CANDIDATE_LIMIT = 3;

    private final FulfillmentWorkJpaRepository fulfillmentWorkJpaRepository;
    private final FulfillmentWorkPersistenceMapper mapper;

    public FulfillmentWorkRepositoryAdapter(FulfillmentWorkJpaRepository fulfillmentWorkJpaRepository,
                                            FulfillmentWorkPersistenceMapper mapper) {
        this.fulfillmentWorkJpaRepository = fulfillmentWorkJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public FulfillmentWork save(FulfillmentWork work) {
        return mapper.toDomain(fulfillmentWorkJpaRepository.save(mapper.toEntity(work)));
    }

    @Override
    public Optional<FulfillmentWork> findByOrderId(OrderId orderId) {
        return fulfillmentWorkJpaRepository.findByOrderId(orderId.value().toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<FulfillmentWork> findById(FulfillmentId id) {
        return fulfillmentWorkJpaRepository.findById(id.value().toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<FulfillmentWork> claimNextPending(ClockProvider clock) {
        Instant now = clock.now();
        List<String> candidateIds = fulfillmentWorkJpaRepository.findPendingIdsOrdered(
                FulfillmentStatus.PENDING,
                PageRequest.of(0, CLAIM_CANDIDATE_LIMIT));
        for (String candidateId : candidateIds) {
            int updated = fulfillmentWorkJpaRepository.tryClaimPending(
                    candidateId,
                    FulfillmentStatus.PENDING,
                    FulfillmentStatus.PROCESSING,
                    now,
                    now);
            if (updated == 1) {
                return fulfillmentWorkJpaRepository.findById(candidateId).map(mapper::toDomain);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<FulfillmentId> findStaleProcessingIds(Instant claimedBefore, int limit) {
        return fulfillmentWorkJpaRepository
                .findStaleProcessingIdsOrdered(
                        FulfillmentStatus.PROCESSING,
                        claimedBefore,
                        PageRequest.of(0, limit))
                .stream()
                .map(FulfillmentId::of)
                .toList();
    }

    @Override
    public Optional<FulfillmentWork> tryMarkStaleProcessingUnknown(
            FulfillmentId id, Instant claimedBefore, ClockProvider clock) {
        Instant now = clock.now();
        int updated = fulfillmentWorkJpaRepository.tryMarkStaleProcessingUnknown(
                id.value().toString(),
                FulfillmentStatus.PROCESSING,
                FulfillmentStatus.UNKNOWN,
                claimedBefore,
                FulfillmentWork.STALE_PROCESSING_ERROR_CODE,
                FulfillmentWork.STALE_PROCESSING_ERROR_MESSAGE,
                now);
        if (updated != 1) {
            return Optional.empty();
        }
        return fulfillmentWorkJpaRepository.findById(id.value().toString()).map(mapper::toDomain);
    }
}
