package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimId;
import com.takarub.esim.commerce.infrastructure.persistence.entity.ProvisionedEsimJpaEntity;

/**
 * Translates between {@link ProvisionedEsim} and {@link ProvisionedEsimJpaEntity}.
 */
@Component
public class ProvisionedEsimPersistenceMapper {

    public ProvisionedEsimJpaEntity toEntity(ProvisionedEsim esim) {
        return new ProvisionedEsimJpaEntity(
                esim.id().value().toString(),
                esim.orderId().value().toString(),
                esim.fulfillmentWorkId().value().toString(),
                esim.supplierKey(),
                esim.remoteProductId(),
                esim.supplierOrderId(),
                esim.iccid(),
                esim.smdpAddress(),
                esim.activationCode(),
                esim.pin(),
                esim.puk(),
                esim.qrString(),
                esim.createdAt(),
                esim.updatedAt());
    }

    public ProvisionedEsim toDomain(ProvisionedEsimJpaEntity entity) {
        return ProvisionedEsim.reconstitute(
                ProvisionedEsimId.of(entity.getId()),
                OrderId.of(entity.getOrderId()),
                FulfillmentId.of(entity.getFulfillmentWorkId()),
                entity.getSupplierKey(),
                entity.getRemoteProductId(),
                entity.getSupplierOrderId(),
                entity.getIccid(),
                entity.getSmdpAddress(),
                entity.getActivationCode(),
                entity.getPin(),
                entity.getPuk(),
                entity.getQrString(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
