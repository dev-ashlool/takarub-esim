package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.OrderJpaRepository;

/**
 * Outbound adapter implementing the {@link OrderRepository} domain port over Spring Data JPA. Pure
 * translation and delegation; repository calls join the active transaction when present.
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper mapper;

    public OrderRepositoryAdapter(OrderJpaRepository orderJpaRepository, OrderPersistenceMapper mapper) {
        this.orderJpaRepository = orderJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        return mapper.toDomain(orderJpaRepository.save(mapper.toEntity(order)));
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderJpaRepository.findById(orderId.value().toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByCartId(CartId cartId) {
        return orderJpaRepository.findByCartId(cartId.value().toString()).map(mapper::toDomain);
    }
}
