package com.takarub.esim.commerce.presentation.cart.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.command.AddItemToCartCommand;
import com.takarub.esim.commerce.application.query.GetCartQuery;
import com.takarub.esim.commerce.application.result.CartItemView;
import com.takarub.esim.commerce.application.result.CartView;
import com.takarub.esim.commerce.presentation.cart.request.AddCartItemRequest;
import com.takarub.esim.commerce.presentation.cart.response.CartItemResponse;
import com.takarub.esim.commerce.presentation.cart.response.CartResponse;

/**
 * Maps between Commerce HTTP DTOs and Application commands / results.
 */
@Component
public class CartMapper {

    public GetCartQuery toGetCartQuery(String userId) {
        return new GetCartQuery(userId);
    }

    public AddItemToCartCommand toAddItemCommand(String userId, AddCartItemRequest request) {
        return new AddItemToCartCommand(userId, request.packageId(), request.quantity());
    }

    public CartResponse toResponse(CartView view) {
        List<CartItemResponse> items = view.items().stream()
                .map(this::toItemResponse)
                .toList();
        return new CartResponse(
                view.id().value().toString(),
                view.userId().value().toString(),
                view.status().name(),
                items,
                view.total(),
                view.currency(),
                view.createdAt(),
                view.updatedAt());
    }

    private CartItemResponse toItemResponse(CartItemView item) {
        return new CartItemResponse(
                item.packageId(),
                item.countryIso(),
                item.countryNameArabic(),
                item.countryNameEnglish(),
                item.locationType().name(),
                item.dataAmount(),
                item.dataUnit().name(),
                item.durationDays(),
                item.unitPrice(),
                item.currency(),
                item.quantity(),
                item.lineTotal());
    }
}
