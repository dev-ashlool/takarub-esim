package com.takarub.esim.commerce.domain.fulfillment;

import java.util.Optional;

import com.takarub.esim.commerce.domain.order.OrderId;

/**
 * Domain repository port for {@link FulfillmentWork}. Implementations live in infrastructure.
 *
 * <p>At most one fulfillment work per {@link OrderId}; uniqueness is enforced by persistence.
 */
public interface FulfillmentWorkRepository {

    FulfillmentWork save(FulfillmentWork work);

    Optional<FulfillmentWork> findByOrderId(OrderId orderId);
}
