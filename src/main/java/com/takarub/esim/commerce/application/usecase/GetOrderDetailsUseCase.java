package com.takarub.esim.commerce.application.usecase;

import java.util.List;

import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.CustomerOrderDetails;
import com.takarub.esim.commerce.application.result.OrderItemView;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ForbiddenException;

/**
 * Returns one order owned by the authenticated customer, with optional fulfillment status.
 */
public class GetOrderDetailsUseCase {

    private final OrderRepository orderRepository;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;

    public GetOrderDetailsUseCase(
            OrderRepository orderRepository, FulfillmentWorkRepository fulfillmentWorkRepository) {
        this.orderRepository = orderRepository;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
    }

    public CustomerOrderDetails execute(UserId userId, OrderId orderId) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundApplicationException(orderId));

        if (!order.userId().equals(userId)) {
            throw new ForbiddenException("Authenticated user does not own this order");
        }

        FulfillmentStatus fulfillmentStatus = fulfillmentWorkRepository
                .findByOrderId(order.id())
                .map(work -> work.status())
                .orElse(null);

        List<OrderItemView> items = order.itemsView().stream().map(OrderItemView::from).toList();
        return new CustomerOrderDetails(
                order.id(),
                order.status(),
                fulfillmentStatus,
                order.totalAmount(),
                order.currency(),
                order.createdAt(),
                order.updatedAt(),
                items);
    }
}
