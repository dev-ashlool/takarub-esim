package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.commerce.application.exception.EsimNotReadyApplicationException;
import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.CustomerEsimActivation;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ForbiddenException;

/**
 * Returns customer eSIM activation data for an owned fulfilled order. Read-only; does not trigger
 * fulfillment or call suppliers.
 */
public class GetCustomerEsimUseCase {

    private final OrderRepository orderRepository;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;
    private final ProvisionedEsimRepository provisionedEsimRepository;

    public GetCustomerEsimUseCase(
            OrderRepository orderRepository,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            ProvisionedEsimRepository provisionedEsimRepository) {
        this.orderRepository = orderRepository;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
        this.provisionedEsimRepository = provisionedEsimRepository;
    }

    public CustomerEsimActivation execute(UserId userId, OrderId orderId) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundApplicationException(orderId));

        if (!order.userId().equals(userId)) {
            throw new ForbiddenException("Authenticated user does not own this order");
        }

        FulfillmentWork work = fulfillmentWorkRepository
                .findByOrderId(orderId)
                .orElseThrow(EsimNotReadyApplicationException::new);

        if (work.status() != FulfillmentStatus.FULFILLED) {
            throw new EsimNotReadyApplicationException();
        }

        ProvisionedEsim esim = provisionedEsimRepository
                .findByOrderId(orderId)
                .orElseThrow(EsimNotReadyApplicationException::new);

        return new CustomerEsimActivation(
                order.id(),
                esim.iccid(),
                esim.qrString(),
                esim.smdpAddress(),
                esim.activationCode(),
                esim.pin(),
                esim.puk());
    }
}
