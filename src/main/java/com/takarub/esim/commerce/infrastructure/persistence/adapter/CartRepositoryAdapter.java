package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.cart.CartStatus;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.CartPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.CartJpaRepository;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Outbound adapter implementing the {@link CartRepository} domain port over Spring Data JPA. Pure
 * translation and delegation; repository calls join the active transaction when present.
 */
@Component
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository cartJpaRepository;
    private final CartPersistenceMapper mapper;

    public CartRepositoryAdapter(CartJpaRepository cartJpaRepository, CartPersistenceMapper mapper) {
        this.cartJpaRepository = cartJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Cart save(Cart cart) {
        return mapper.toDomain(cartJpaRepository.save(mapper.toEntity(cart)));
    }

    @Override
    public Optional<Cart> findById(CartId cartId) {
        return cartJpaRepository.findById(cartId.value().toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<Cart> findOpenByUserId(UserId userId) {
        return cartJpaRepository
                .findByUserIdAndStatus(userId.value().toString(), CartStatus.OPEN)
                .map(mapper::toDomain);
    }
}
