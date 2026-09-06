package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.time.ClockProvider;

@ExtendWith(MockitoExtension.class)
class ReconcileStaleFulfillmentWorkUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-05T13:00:00Z");
    private static final Duration THRESHOLD = Duration.ofMinutes(30);
    private static final Instant CUTOFF = NOW.minus(THRESHOLD);

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private FulfillmentWorkRepository fulfillmentWorkRepository;
    @Mock
    private ClockProvider clock;

    private ReconcileStaleFulfillmentWorkUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReconcileStaleFulfillmentWorkUseCase(
                transactionRunner, fulfillmentWorkRepository, clock, THRESHOLD);
        lenient().doAnswer(invocation -> {
                    Supplier<?> work = invocation.getArgument(0);
                    return work.get();
                }).when(transactionRunner).execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());
        lenient().when(clock.now()).thenReturn(NOW);
    }

    @Test
    void staleProcessingBecomesUnknownWithSafeError() {
        FulfillmentId id = FulfillmentId.of(UUID.randomUUID());
        FulfillmentWork reconciled = FulfillmentWork.reconstitute(
                id,
                OrderId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                FulfillmentStatus.UNKNOWN,
                CUTOFF.minusSeconds(60),
                FulfillmentWork.STALE_PROCESSING_ERROR_CODE,
                FulfillmentWork.STALE_PROCESSING_ERROR_MESSAGE,
                NOW.minusSeconds(3600),
                NOW);

        when(fulfillmentWorkRepository.findStaleProcessingIds(CUTOFF, ReconcileStaleFulfillmentWorkUseCase.BATCH_LIMIT))
                .thenReturn(List.of(id));
        when(fulfillmentWorkRepository.tryMarkStaleProcessingUnknown(id, CUTOFF, clock))
                .thenReturn(Optional.of(reconciled));

        int count = useCase.execute();

        assertThat(count).isEqualTo(1);
        assertThat(reconciled.status()).isEqualTo(FulfillmentStatus.UNKNOWN);
        assertThat(reconciled.lastErrorCode()).isEqualTo(FulfillmentWork.STALE_PROCESSING_ERROR_CODE);
        assertThat(reconciled.lastErrorMessage()).isEqualTo(FulfillmentWork.STALE_PROCESSING_ERROR_MESSAGE);
        verify(fulfillmentWorkRepository).tryMarkStaleProcessingUnknown(id, CUTOFF, clock);
    }

    @Test
    void emptyStaleQueueDoesNothing() {
        when(fulfillmentWorkRepository.findStaleProcessingIds(CUTOFF, ReconcileStaleFulfillmentWorkUseCase.BATCH_LIMIT))
                .thenReturn(List.of());

        assertThat(useCase.execute()).isZero();
        verify(fulfillmentWorkRepository, never()).tryMarkStaleProcessingUnknown(any(), any(), any());
    }

    @Test
    void raceLostCandidateIsSkippedWithoutOverwrite() {
        FulfillmentId id = FulfillmentId.of(UUID.randomUUID());
        when(fulfillmentWorkRepository.findStaleProcessingIds(CUTOFF, ReconcileStaleFulfillmentWorkUseCase.BATCH_LIMIT))
                .thenReturn(List.of(id));
        when(fulfillmentWorkRepository.tryMarkStaleProcessingUnknown(id, CUTOFF, clock))
                .thenReturn(Optional.empty());

        assertThat(useCase.execute()).isZero();
        verify(fulfillmentWorkRepository).tryMarkStaleProcessingUnknown(eq(id), eq(CUTOFF), eq(clock));
    }

    @Test
    void onlyMarksReturnedStaleCandidatesAndSkipsRaceLosers() {
        FulfillmentId staleId = FulfillmentId.of(UUID.randomUUID());
        FulfillmentId racedId = FulfillmentId.of(UUID.randomUUID());
        FulfillmentWork reconciled = FulfillmentWork.reconstitute(
                staleId,
                OrderId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                FulfillmentStatus.UNKNOWN,
                CUTOFF.minusSeconds(120),
                FulfillmentWork.STALE_PROCESSING_ERROR_CODE,
                FulfillmentWork.STALE_PROCESSING_ERROR_MESSAGE,
                NOW.minusSeconds(3600),
                NOW);

        when(fulfillmentWorkRepository.findStaleProcessingIds(CUTOFF, ReconcileStaleFulfillmentWorkUseCase.BATCH_LIMIT))
                .thenReturn(List.of(staleId, racedId));
        when(fulfillmentWorkRepository.tryMarkStaleProcessingUnknown(staleId, CUTOFF, clock))
                .thenReturn(Optional.of(reconciled));
        when(fulfillmentWorkRepository.tryMarkStaleProcessingUnknown(racedId, CUTOFF, clock))
                .thenReturn(Optional.empty());

        assertThat(useCase.execute()).isEqualTo(1);
        verify(fulfillmentWorkRepository).tryMarkStaleProcessingUnknown(staleId, CUTOFF, clock);
        verify(fulfillmentWorkRepository).tryMarkStaleProcessingUnknown(racedId, CUTOFF, clock);
    }

    @Test
    void cutoffIsNowMinusConfiguredThreshold() {
        when(fulfillmentWorkRepository.findStaleProcessingIds(any(), anyInt())).thenReturn(List.of());

        useCase.execute();

        verify(fulfillmentWorkRepository)
                .findStaleProcessingIds(eq(CUTOFF), eq(ReconcileStaleFulfillmentWorkUseCase.BATCH_LIMIT));
    }
}
