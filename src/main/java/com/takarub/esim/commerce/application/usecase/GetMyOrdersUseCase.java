package com.takarub.esim.commerce.application.usecase;

import java.util.List;

import com.takarub.esim.commerce.application.result.CustomerOrderSummary;
import com.takarub.esim.commerce.application.result.OrderItemView;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Returns the authenticated customer's orders newest-first with optional fulfillment status.
 */
public class GetMyOrdersUseCase {

    private final OrderRepository orderRepository;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;

    public GetMyOrdersUseCase(
            OrderRepository orderRepository, FulfillmentWorkRepository fulfillmentWorkRepository) {
        this.orderRepository = orderRepository;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
    }

    public List<CustomerOrderSummary> execute(UserId userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private CustomerOrderSummary toSummary(Order order) {
        FulfillmentStatus fulfillmentStatus = fulfillmentWorkRepository
                .findByOrderId(order.id())
                .map(work -> work.status())
                .orElse(null);
        List<OrderItemView> items = order.itemsView().stream().map(OrderItemView::from).toList();
        return new CustomerOrderSummary(
                order.id(),
                order.status(),
                fulfillmentStatus,
                order.totalAmount(),
                order.currency(),
                order.createdAt(),
                items);
    }
}
