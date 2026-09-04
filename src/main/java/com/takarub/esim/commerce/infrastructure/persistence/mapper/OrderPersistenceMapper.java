package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderItem;
import com.takarub.esim.commerce.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.entity.OrderJpaEntity;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Translates between the {@link Order} aggregate and {@link OrderJpaEntity}. Entity → domain uses
 * {@link Order#reconstitute} and {@link OrderItem#reconstitute}; no reflection and no aggregate
 * mutators.
 */
@Component
public class OrderPersistenceMapper {

    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(
                order.id().value().toString(),
                order.cartId().value().toString(),
                order.userId().value().toString(),
                order.checkoutRequestId().value(),
                order.status(),
                order.totalAmount(),
                order.currency(),
                order.createdAt(),
                order.updatedAt());

        for (OrderItem item : order.itemsView()) {
            OrderItemJpaEntity line = new OrderItemJpaEntity(
                    item.packageId(),
                    item.countryIso(),
                    item.countryNameArabic(),
                    item.countryNameEnglish(),
                    item.locationType(),
                    item.dataAmount(),
                    item.dataUnit(),
                    item.durationDays(),
                    item.unitPrice(),
                    item.currency(),
                    item.quantity());
            entity.addItem(line);
        }
        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemJpaEntity line : entity.getItems()) {
            items.add(OrderItem.reconstitute(
                    line.getPackageId(),
                    line.getCountryIso(),
                    line.getCountryNameArabic(),
                    line.getCountryNameEnglish(),
                    line.getLocationType(),
                    line.getDataAmount(),
                    line.getDataUnit(),
                    line.getDurationDays(),
                    line.getUnitPrice(),
                    line.getCurrency(),
                    line.getQuantity()));
        }

        return Order.reconstitute(
                OrderId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                CartId.of(entity.getCartId()),
                UserId.of(entity.getUserId()),
                CheckoutRequestId.of(entity.getCheckoutRequestId()),
                entity.getStatus(),
                items,
                entity.getTotalAmount(),
                entity.getCurrency());
    }
}
