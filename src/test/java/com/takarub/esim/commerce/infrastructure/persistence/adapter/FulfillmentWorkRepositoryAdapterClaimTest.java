package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.infrastructure.persistence.entity.FulfillmentWorkJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.FulfillmentWorkPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.FulfillmentWorkJpaRepository;
import com.takarub.esim.identity.shared.time.ClockProvider;

@ExtendWith(MockitoExtension.class)
class FulfillmentWorkRepositoryAdapterClaimTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

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
        when(clock.now()).thenReturn(NOW);
    }

    @Test
    void emptyCandidatesReturnsEmpty() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(adapter.claimNextPending(clock)).isEmpty();
        verify(jpaRepository, never()).tryClaimPending(any(), any(), any(), any(), any());
    }

    @Test
    void firstCandidateWins() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of("id-1", "id-2", "id-3"));
        when(jpaRepository.tryClaimPending(
                        "id-1",
                        FulfillmentStatus.PENDING,
                        FulfillmentStatus.PROCESSING,
                        NOW,
                        NOW))
                .thenReturn(1);
        when(jpaRepository.findById("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainWork);
        when(domainWork.status()).thenReturn(FulfillmentStatus.PROCESSING);

        Optional<FulfillmentWork> claimed = adapter.claimNextPending(clock);

        assertThat(claimed).contains(domainWork);
        assertThat(claimed.get().status()).isEqualTo(FulfillmentStatus.PROCESSING);
        verify(jpaRepository, times(1)).tryClaimPending(any(), any(), any(), any(), any());
        verify(jpaRepository).findById("id-1");
        verify(jpaRepository, never()).tryClaimPending(eq("id-2"), any(), any(), any(), any());
    }

    @Test
    void firstLosesSecondWins() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of("id-1", "id-2"));
        when(jpaRepository.tryClaimPending(
                        "id-1",
                        FulfillmentStatus.PENDING,
                        FulfillmentStatus.PROCESSING,
                        NOW,
                        NOW))
                .thenReturn(0);
        when(jpaRepository.tryClaimPending(
                        "id-2",
                        FulfillmentStatus.PENDING,
                        FulfillmentStatus.PROCESSING,
                        NOW,
                        NOW))
                .thenReturn(1);
        when(jpaRepository.findById("id-2")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainWork);

        assertThat(adapter.claimNextPending(clock)).contains(domainWork);
        verify(jpaRepository, times(2)).tryClaimPending(any(), any(), any(), any(), any());
        verify(jpaRepository).findById("id-2");
    }

    @Test
    void allSelectedCandidatesLoseReturnsEmpty() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of("id-1", "id-2", "id-3"));
        when(jpaRepository.tryClaimPending(any(), any(), any(), any(), any())).thenReturn(0);

        assertThat(adapter.claimNextPending(clock)).isEmpty();
        verify(jpaRepository, times(3)).tryClaimPending(any(), any(), any(), any(), any());
        verify(jpaRepository, never()).findById(any());
    }

    @Test
    void orderingQueryReceivesPageSizeThreeAndPendingStatus() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        adapter.claimNextPending(clock);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findPendingIdsOrdered(
                eq(FulfillmentStatus.PENDING), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void pendingPredicatePassedToUpdate() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of("id-1"));
        when(jpaRepository.tryClaimPending(any(), any(), any(), any(), any())).thenReturn(0);

        adapter.claimNextPending(clock);

        verify(jpaRepository).tryClaimPending(
                eq("id-1"),
                eq(FulfillmentStatus.PENDING),
                eq(FulfillmentStatus.PROCESSING),
                eq(NOW),
                eq(NOW));
    }

    @Test
    void exactlyMaxThreeCandidatesAttemptedNeverFourth() {
        when(jpaRepository.findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of("id-1", "id-2", "id-3"));
        when(jpaRepository.tryClaimPending(any(), any(), any(), any(), any())).thenReturn(0);

        adapter.claimNextPending(clock);

        verify(jpaRepository, times(3)).tryClaimPending(any(), any(), any(), any(), any());
        verify(jpaRepository, never()).tryClaimPending(eq("id-4"), any(), any(), any(), any());
        verify(jpaRepository, times(1))
                .findPendingIdsOrdered(eq(FulfillmentStatus.PENDING), any(Pageable.class));
    }
}
