package com.takarub.esim.commerce.domain.fulfillment;

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
}
