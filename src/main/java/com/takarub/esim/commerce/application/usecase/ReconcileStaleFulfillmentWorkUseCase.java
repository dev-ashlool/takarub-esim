package com.takarub.esim.commerce.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Conservatively reconciles stale PROCESSING fulfillment work to UNKNOWN.
 *
 * <p>Does not call the supplier, does not retry purchases, and never moves UNKNOWN back to PENDING.
 * Local DB cannot prove whether a supplier request was already sent.
 */
public class ReconcileStaleFulfillmentWorkUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcileStaleFulfillmentWorkUseCase.class);

    /** Small fixed batch; mirrors claim candidate sizing style without over-engineering. */
    static final int BATCH_LIMIT = 10;

    private final TransactionRunner transactionRunner;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;
    private final ClockProvider clock;
    private final Duration staleProcessingThreshold;

    public ReconcileStaleFulfillmentWorkUseCase(
            TransactionRunner transactionRunner,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            ClockProvider clock,
            Duration staleProcessingThreshold) {
        this.transactionRunner = transactionRunner;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
        this.clock = clock;
        this.staleProcessingThreshold = staleProcessingThreshold;
    }

    /**
     * @return number of rows successfully transitioned PROCESSING → UNKNOWN in this tick
     */
    public int execute() {
        Instant cutoff = clock.now().minus(staleProcessingThreshold);
        List<FulfillmentId> candidates = transactionRunner.execute(
                () -> fulfillmentWorkRepository.findStaleProcessingIds(cutoff, BATCH_LIMIT));

        int reconciled = 0;
        for (FulfillmentId id : candidates) {
            Optional<FulfillmentWork> updated = transactionRunner.execute(
                    () -> fulfillmentWorkRepository.tryMarkStaleProcessingUnknown(id, cutoff, clock));
            if (updated.isEmpty()) {
                continue;
            }
            FulfillmentWork work = updated.get();
            reconciled++;
            log.info(
                    "Reconciled stale fulfillment PROCESSING->UNKNOWN fulfillmentWorkId={} orderId={} claimedAt={} newStatus={}",
                    work.id().value(),
                    work.orderId().value(),
                    work.claimedAt(),
                    FulfillmentStatus.UNKNOWN);
        }
        return reconciled;
    }
}
