package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;

/**
 * Thrown when an Order cannot be loaded for the requested id.
 */
public class OrderNotFoundApplicationException extends ResourceNotFoundException {

    public OrderNotFoundApplicationException(OrderId orderId) {
        super(CommerceApplicationErrorCode.ORDER_NOT_FOUND,
                "Order " + orderId.value() + " was not found.");
    }
}
