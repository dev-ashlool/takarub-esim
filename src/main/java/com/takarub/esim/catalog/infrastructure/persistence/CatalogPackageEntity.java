package com.takarub.esim.catalog.infrastructure.persistence;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of a unified client-facing catalog package.
 */
@Entity
@Table(name = "catalog_packages")
public class CatalogPackageEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso", nullable = false)
    private CountryEntity country;

    @Column(name = "data_amount", nullable = false)
    private int dataAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_unit", length = 10, nullable = false)
    private DataUnit dataUnit;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 10, nullable = false)
    private LocationType locationType = LocationType.COUNTRY;

    protected CatalogPackageEntity() {
        // Required by JPA.
    }

    public CatalogPackageEntity(String id, CountryEntity country, int dataAmount, DataUnit dataUnit,
                                int durationDays, boolean available) {
        this.id = id;
        this.country = country;
        this.dataAmount = dataAmount;
        this.dataUnit = dataUnit;
        this.durationDays = durationDays;
        this.available = available;
        this.locationType = country.getLocationType();
    }

    public String getId() {
        return id;
    }

    public CountryEntity getCountry() {
        return country;
    }

    public int getDataAmount() {
        return dataAmount;
    }

    public DataUnit getDataUnit() {
        return dataUnit;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocationType getLocationType() {
        return locationType;
    }
}
