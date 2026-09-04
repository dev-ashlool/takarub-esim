package com.takarub.esim.commerce.infrastructure.persistence.entity;

import java.math.BigDecimal;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA persistence representation of an order line commercial snapshot. Does not store derived
 * {@code lineTotal}.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity orderEntity;

    @Column(name = "package_id", length = 36, nullable = false)
    private String packageId;

    @Column(name = "country_iso", length = 10, nullable = false)
    private String countryIso;

    @Column(name = "country_name_arabic", length = 100, nullable = false)
    private String countryNameArabic;

    @Column(name = "country_name_english", length = 100, nullable = false)
    private String countryNameEnglish;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 32, nullable = false)
    private LocationType locationType;

    @Column(name = "data_amount", nullable = false)
    private int dataAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_unit", length = 10, nullable = false)
    private DataUnit dataUnit;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "unit_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "currency", length = 16, nullable = false)
    private String currency;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public OrderItemJpaEntity(
            String packageId,
            String countryIso,
            String countryNameArabic,
            String countryNameEnglish,
            LocationType locationType,
            int dataAmount,
            DataUnit dataUnit,
            int durationDays,
            BigDecimal unitPrice,
            String currency,
            int quantity) {
        this.packageId = packageId;
        this.countryIso = countryIso;
        this.countryNameArabic = countryNameArabic;
        this.countryNameEnglish = countryNameEnglish;
        this.locationType = locationType;
        this.dataAmount = dataAmount;
        this.dataUnit = dataUnit;
        this.durationDays = durationDays;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.quantity = quantity;
    }

    void setOrder(OrderJpaEntity orderEntity) {
        this.orderEntity = orderEntity;
    }
}
