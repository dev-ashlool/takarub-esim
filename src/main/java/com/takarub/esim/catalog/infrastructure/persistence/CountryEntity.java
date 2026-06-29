package com.takarub.esim.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of a catalog country (ISO alpha-2).
 */
@Entity
@Table(name = "catalog_countries")
public class CountryEntity {

    @Id
    @Column(name = "id", length = 2, nullable = false, updatable = false)
    private String id;

    @Column(name = "arabic_name", length = 100, nullable = false)
    private String arabicName;

    @Column(name = "english_name", length = 100, nullable = false)
    private String englishName;

    @Column(name = "flag_image_url", length = 255)
    private String flagImageUrl;

    protected CountryEntity() {
        // Required by JPA.
    }

    public CountryEntity(String id, String arabicName, String englishName, String flagImageUrl) {
        this.id = id;
        this.arabicName = arabicName;
        this.englishName = englishName;
        this.flagImageUrl = flagImageUrl;
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
}
