package com.takarub.esim.commerce.presentation.order.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.result.CustomerOrderDetails;
import com.takarub.esim.commerce.application.result.CustomerOrderSummary;
import com.takarub.esim.commerce.application.result.OrderItemView;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.presentation.order.response.CustomerOrderItemResponse;
import com.takarub.esim.commerce.presentation.order.response.MyOrderSummaryResponse;
import com.takarub.esim.commerce.presentation.order.response.OrderDetailsResponse;

/**
 * Maps customer order application results to REST DTOs. Omits supplier and provisioning secrets.
 */
@Component
public class CustomerOrderMapper {

    public List<MyOrderSummaryResponse> toSummaryResponses(List<CustomerOrderSummary> summaries) {
        return summaries.stream().map(this::toSummaryResponse).toList();
    }

    public MyOrderSummaryResponse toSummaryResponse(CustomerOrderSummary summary) {
        return new MyOrderSummaryResponse(
                summary.orderId().value().toString(),
                summary.orderStatus().name(),
                fulfillmentStatusName(summary.fulfillmentStatus()),
                summary.totalAmount(),
                summary.currency(),
                summary.createdAt(),
                toItemResponses(summary.items()));
    }

    public OrderDetailsResponse toDetailsResponse(CustomerOrderDetails details) {
        return new OrderDetailsResponse(
                details.orderId().value().toString(),
                details.orderStatus().name(),
                fulfillmentStatusName(details.fulfillmentStatus()),
                details.totalAmount(),
                details.currency(),
                details.createdAt(),
                details.updatedAt(),
                toItemResponses(details.items()));
    }

    private static List<CustomerOrderItemResponse> toItemResponses(List<OrderItemView> items) {
        return items.stream().map(CustomerOrderMapper::toItemResponse).toList();
    }

    private static CustomerOrderItemResponse toItemResponse(OrderItemView item) {
        return new CustomerOrderItemResponse(
                item.packageId(),
                item.countryIso(),
                item.countryNameArabic(),
                item.countryNameEnglish(),
                item.dataAmount(),
                item.dataUnit().name(),
                item.durationDays(),
                item.unitPrice(),
                item.quantity(),
                item.lineTotal());
    }

    private static String fulfillmentStatusName(FulfillmentStatus status) {
        return status == null ? null : status.name();
    }
}
