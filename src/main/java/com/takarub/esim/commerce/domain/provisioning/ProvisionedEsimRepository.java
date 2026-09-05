package com.takarub.esim.commerce.domain.provisioning;

import java.util.Optional;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;

/**
 * Domain repository port for {@link ProvisionedEsim}. Implementations live in infrastructure.
 */
public interface ProvisionedEsimRepository {

    ProvisionedEsim save(ProvisionedEsim esim);

    Optional<ProvisionedEsim> findByOrderId(OrderId orderId);

    Optional<ProvisionedEsim> findByFulfillmentWorkId(FulfillmentId fulfillmentWorkId);
}
