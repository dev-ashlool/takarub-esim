package com.takarub.esim.commerce.domain.fulfillment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Domain repository port for {@link FulfillmentWork}. Implementations live in infrastructure.
 *
 * <p>At most one fulfillment work per {@link OrderId}; uniqueness is enforced by persistence.
 */
public interface FulfillmentWorkRepository {

    FulfillmentWork save(FulfillmentWork work);

    Optional<FulfillmentWork> findByOrderId(OrderId orderId);

    Optional<FulfillmentWork> findById(FulfillmentId id);

    /**
     * Atomically claims the oldest PENDING work (up to three candidates considered). Returns empty
     * when the queue is empty or all candidates lose a concurrent race.
     */
    Optional<FulfillmentWork> claimNextPending(ClockProvider clock);

    /**
     * Returns up to {@code limit} PROCESSING work ids with {@code claimedAt} strictly before
     * {@code claimedBefore}, ordered by claimedAt ASC then id ASC.
     */
    List<FulfillmentId> findStaleProcessingIds(Instant claimedBefore, int limit);

    /**
     * Conditionally marks PROCESSING work UNKNOWN when still PROCESSING and {@code claimedAt}
     * strictly before {@code claimedBefore}. Returns empty when the row lost a race or is no longer
     * eligible.
     */
    Optional<FulfillmentWork> tryMarkStaleProcessingUnknown(
            FulfillmentId id,
            Instant claimedBefore,
            ClockProvider clock);
}
