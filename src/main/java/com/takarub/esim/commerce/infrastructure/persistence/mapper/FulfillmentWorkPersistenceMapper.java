package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.infrastructure.persistence.entity.FulfillmentWorkJpaEntity;

/**
 * Translates between {@link FulfillmentWork} and {@link FulfillmentWorkJpaEntity}.
 */
@Component
public class FulfillmentWorkPersistenceMapper {

    public FulfillmentWorkJpaEntity toEntity(FulfillmentWork work) {
        return new FulfillmentWorkJpaEntity(
                work.id().value().toString(),
                work.orderId().value().toString(),
                work.supplierKey(),
                work.remoteProductId(),
                work.status(),
                work.claimedAt(),
                work.lastErrorCode(),
                work.lastErrorMessage(),
                work.createdAt(),
                work.updatedAt());
    }

    public FulfillmentWork toDomain(FulfillmentWorkJpaEntity entity) {
        return FulfillmentWork.reconstitute(
                FulfillmentId.of(entity.getId()),
                OrderId.of(entity.getOrderId()),
                entity.getSupplierKey(),
                entity.getRemoteProductId(),
                entity.getStatus(),
                entity.getClaimedAt(),
                entity.getLastErrorCode(),
                entity.getLastErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
