package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.ProvisionedEsimPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.ProvisionedEsimJpaRepository;

/**
 * Outbound adapter implementing {@link ProvisionedEsimRepository} over Spring Data JPA.
 */
@Component
public class ProvisionedEsimRepositoryAdapter implements ProvisionedEsimRepository {

    private final ProvisionedEsimJpaRepository provisionedEsimJpaRepository;
    private final ProvisionedEsimPersistenceMapper mapper;

    public ProvisionedEsimRepositoryAdapter(ProvisionedEsimJpaRepository provisionedEsimJpaRepository,
                                            ProvisionedEsimPersistenceMapper mapper) {
        this.provisionedEsimJpaRepository = provisionedEsimJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ProvisionedEsim save(ProvisionedEsim esim) {
        return mapper.toDomain(provisionedEsimJpaRepository.save(mapper.toEntity(esim)));
    }

    @Override
    public Optional<ProvisionedEsim> findByOrderId(OrderId orderId) {
        return provisionedEsimJpaRepository.findByOrderId(orderId.value().toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<ProvisionedEsim> findByFulfillmentWorkId(FulfillmentId fulfillmentWorkId) {
        return provisionedEsimJpaRepository
                .findByFulfillmentWorkId(fulfillmentWorkId.value().toString())
                .map(mapper::toDomain);
    }
}
