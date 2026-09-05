package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.FulfillmentWorkPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.FulfillmentWorkJpaRepository;

/**
 * Outbound adapter implementing {@link FulfillmentWorkRepository} over Spring Data JPA.
 */
@Component
public class FulfillmentWorkRepositoryAdapter implements FulfillmentWorkRepository {

    private final FulfillmentWorkJpaRepository fulfillmentWorkJpaRepository;
    private final FulfillmentWorkPersistenceMapper mapper;

    public FulfillmentWorkRepositoryAdapter(FulfillmentWorkJpaRepository fulfillmentWorkJpaRepository,
                                            FulfillmentWorkPersistenceMapper mapper) {
        this.fulfillmentWorkJpaRepository = fulfillmentWorkJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public FulfillmentWork save(FulfillmentWork work) {
        return mapper.toDomain(fulfillmentWorkJpaRepository.save(mapper.toEntity(work)));
    }

    @Override
    public Optional<FulfillmentWork> findByOrderId(OrderId orderId) {
        return fulfillmentWorkJpaRepository.findByOrderId(orderId.value().toString()).map(mapper::toDomain);
    }
}
