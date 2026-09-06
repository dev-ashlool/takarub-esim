package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.infrastructure.persistence.entity.FulfillmentWorkJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.FulfillmentWorkPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.FulfillmentWorkJpaRepository;
import com.takarub.esim.identity.shared.time.ClockProvider;

@ExtendWith(MockitoExtension.class)
class FulfillmentWorkRepositoryAdapterStaleReconciliationTest {

    private static final Instant NOW = Instant.parse("2026-09-05T13:00:00Z");
    private static final Instant CUTOFF = Instant.parse("2026-09-05T12:30:00Z");

    @Mock
    private FulfillmentWorkJpaRepository jpaRepository;
    @Mock
    private FulfillmentWorkPersistenceMapper mapper;
    @Mock
    private ClockProvider clock;
    @Mock
    private FulfillmentWork domainWork;
    @Mock
    private FulfillmentWorkJpaEntity entity;

    private FulfillmentWorkRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FulfillmentWorkRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void findStaleProcessingIdsOrdersAndMaps() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        when(jpaRepository.findStaleProcessingIdsOrdered(
                        eq(FulfillmentStatus.PROCESSING), eq(CUTOFF), any(Pageable.class)))
                .thenReturn(List.of(id1, id2));

        List<FulfillmentId> ids = adapter.findStaleProcessingIds(CUTOFF, 10);

        assertThat(ids).containsExactly(FulfillmentId.of(id1), FulfillmentId.of(id2));
    }

    @Test
    void tryMarkStaleProcessingUnknownSucceedsConditionally() {
        when(clock.now()).thenReturn(NOW);
        String id = UUID.randomUUID().toString();
        when(jpaRepository.tryMarkStaleProcessingUnknown(
                        id,
                        FulfillmentStatus.PROCESSING,
                        FulfillmentStatus.UNKNOWN,
                        CUTOFF,
                        FulfillmentWork.STALE_PROCESSING_ERROR_CODE,
                        FulfillmentWork.STALE_PROCESSING_ERROR_MESSAGE,
                        NOW))
                .thenReturn(1);
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainWork);

        Optional<FulfillmentWork> result =
                adapter.tryMarkStaleProcessingUnknown(FulfillmentId.of(id), CUTOFF, clock);

        assertThat(result).contains(domainWork);
    }

    @Test
    void tryMarkStaleProcessingUnknownRaceReturnsEmptyWithoutReload() {
        when(clock.now()).thenReturn(NOW);
        String id = UUID.randomUUID().toString();
        when(jpaRepository.tryMarkStaleProcessingUnknown(
                        id,
                        FulfillmentStatus.PROCESSING,
                        FulfillmentStatus.UNKNOWN,
                        CUTOFF,
                        FulfillmentWork.STALE_PROCESSING_ERROR_CODE,
                        FulfillmentWork.STALE_PROCESSING_ERROR_MESSAGE,
                        NOW))
                .thenReturn(0);

        Optional<FulfillmentWork> result =
                adapter.tryMarkStaleProcessingUnknown(FulfillmentId.of(id), CUTOFF, clock);

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).findById(any());
    }
}
