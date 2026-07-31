package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.cart.CartItem;
import com.takarub.esim.commerce.infrastructure.persistence.entity.CartItemJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.entity.CartJpaEntity;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Translates between the {@link Cart} aggregate and {@link CartJpaEntity}. Entity → domain uses
 * {@link Cart#reconstitute} and {@link CartItem#reconstitute}; no reflection and no aggregate
 * mutators ({@code addItem}, {@code updateQuantity}, etc.).
 */
@Component
public class CartPersistenceMapper {

    public CartJpaEntity toEntity(Cart cart) {
        CartJpaEntity entity = new CartJpaEntity(
                cart.id().value().toString(),
                cart.userId().value().toString(),
                cart.status(),
                cart.createdAt(),
                cart.updatedAt());

        for (CartItem item : cart.itemsView()) {
            CartItemJpaEntity line = new CartItemJpaEntity(
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

    public Cart toDomain(CartJpaEntity entity) {
        List<CartItem> items = new ArrayList<>();
        for (CartItemJpaEntity line : entity.getItems()) {
            items.add(CartItem.reconstitute(
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

        return Cart.reconstitute(
                CartId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                UserId.of(entity.getUserId()),
                entity.getStatus(),
                items);
    }
}
