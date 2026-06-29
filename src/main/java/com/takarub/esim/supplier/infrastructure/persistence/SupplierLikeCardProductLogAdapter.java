package com.takarub.esim.supplier.infrastructure.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takarub.esim.supplier.application.port.SupplierLikeCardProductLogPort;
import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;

/**
 * Persists parsed LikeCard catalog rows into {@code supplier_likecard_products}.
 */
@Component
public class SupplierLikeCardProductLogAdapter implements SupplierLikeCardProductLogPort {

    private final SupplierLikeCardProductJpaRepository repository;
    private final ObjectMapper objectMapper;

    public SupplierLikeCardProductLogAdapter(SupplierLikeCardProductJpaRepository repository,
                                             ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveOrUpdate(RawSupplierProduct parsed, Instant syncedAt) {
        String rawPayload = serializeProduct(parsed);
        SupplierLikeCardProductEntity entity = repository.findByRemoteProductId(parsed.id())
                .orElseGet(() -> new SupplierLikeCardProductEntity(
                        parsed.id(),
                        parsed.countryIso(),
                        rawPayload,
                        parsed.costPrice(),
                        parsed.costCurrency(),
                        parsed.dataAmount(),
                        parsed.dataUnit().name(),
                        parsed.durationDays(),
                        syncedAt));

        entity.setCountryIso(parsed.countryIso());
        entity.setRawPayload(rawPayload);
        entity.setPriceWithVat(parsed.costPrice());
        entity.setCurrencyCode(parsed.costCurrency());
        entity.setDataAmount(parsed.dataAmount());
        entity.setDataUnit(parsed.dataUnit().name());
        entity.setDurationDays(parsed.durationDays());
        entity.setLastSyncedAt(syncedAt);
        repository.save(entity);
    }

    private String serializeProduct(RawSupplierProduct product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException ex) {
            throw new SupplierApiException("Failed to serialize LikeCard product payload", ex);
        }
    }
}
