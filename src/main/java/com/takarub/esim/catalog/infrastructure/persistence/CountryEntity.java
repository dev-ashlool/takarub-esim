package com.takarub.esim.catalog.infrastructure.persistence;

import com.takarub.esim.supplier.domain.model.LocationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of a catalog location (country or region).
 */
@Entity
@Table(name = "catalog_countries")
public class CountryEntity {

    @Id
    @Column(name = "id", length = 50, nullable = false, updatable = false)
    private String id;

    @Column(name = "arabic_name", length = 100, nullable = false)
    private String arabicName;

    @Column(name = "english_name", length = 100, nullable = false)
    private String englishName;

    @Column(name = "flag_image_url", length = 255)
    private String flagImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 10, nullable = false)
    private LocationType locationType = LocationType.COUNTRY;

    protected CountryEntity() {
        // Required by JPA.
    }

    public CountryEntity(String id, String arabicName, String englishName, String flagImageUrl) {
        this.id = id;
        this.arabicName = arabicName;
        this.englishName = englishName;
        this.flagImageUrl = flagImageUrl;
    }

    public CountryEntity(String id, String arabicName, String englishName, String flagImageUrl, LocationType locationType) {
        this.id = id;
        this.arabicName = arabicName;
        this.englishName = englishName;
        this.flagImageUrl = flagImageUrl;
        this.locationType = locationType;
    }

    public String getId() {
        return id;
    }

    public String getArabicName() {
        return arabicName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getFlagImageUrl() {
        return flagImageUrl;
    }

    public void setArabicName(String arabicName) {
        this.arabicName = arabicName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public void setFlagImageUrl(String flagImageUrl) {
        this.flagImageUrl = flagImageUrl;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }
}
