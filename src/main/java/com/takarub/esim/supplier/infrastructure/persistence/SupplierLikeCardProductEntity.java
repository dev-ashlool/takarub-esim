package com.takarub.esim.supplier.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Raw LikeCard product snapshot persisted on each catalog synchronization run.
 */
@Entity
@Table(name = "supplier_likecard_products")
public class SupplierLikeCardProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "remote_product_id", length = 50, nullable = false, unique = true)
    private String remoteProductId;

    @Column(name = "country_iso", length = 50, nullable = false)
    private String countryIso;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "price_with_vat", precision = 12, scale = 4)
    private BigDecimal priceWithVat;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "data_amount")
    private Integer dataAmount;

    @Column(name = "data_unit", length = 10)
    private String dataUnit;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    protected SupplierLikeCardProductEntity() {
        // Required by JPA.
    }

    public SupplierLikeCardProductEntity(String remoteProductId, String countryIso, String rawPayload,
                                         BigDecimal priceWithVat, String currencyCode, Integer dataAmount,
                                         String dataUnit, Integer durationDays, Instant lastSyncedAt) {
        this.remoteProductId = remoteProductId;
        this.countryIso = countryIso;
        this.rawPayload = rawPayload;
        this.priceWithVat = priceWithVat;
        this.currencyCode = currencyCode;
        this.dataAmount = dataAmount;
        this.dataUnit = dataUnit;
        this.durationDays = durationDays;
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getRemoteProductId() {
        return remoteProductId;
    }

    public void setCountryIso(String countryIso) {
        this.countryIso = countryIso;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public void setPriceWithVat(BigDecimal priceWithVat) {
        this.priceWithVat = priceWithVat;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setDataAmount(Integer dataAmount) {
        this.dataAmount = dataAmount;
    }

    public void setDataUnit(String dataUnit) {
        this.dataUnit = dataUnit;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
