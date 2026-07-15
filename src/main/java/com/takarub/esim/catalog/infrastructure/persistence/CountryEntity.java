package com.takarub.esim.catalog.infrastructure.persistence;

import com.takarub.esim.catalog.domain.service.CatalogSlugGenerator;
import com.takarub.esim.supplier.domain.model.LocationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of a catalog location (country or region).
 * {@code slug} is set once on first persist and must remain stable for SEO.
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

    @Column(name = "slug", length = 255, nullable = false, unique = true, updatable = false)
    private String slug;

    protected CountryEntity() {
        // Required by JPA.
    }

    public CountryEntity(String id, String arabicName, String englishName, String flagImageUrl) {
        this.id = id;
        this.arabicName = arabicName;
        this.englishName = englishName;
        this.flagImageUrl = flagImageUrl;
        this.slug = CatalogSlugGenerator.fromEnglishName(englishName);
    }

    public CountryEntity(String id, String arabicName, String englishName, String flagImageUrl, LocationType locationType) {
        this.id = id;
        this.arabicName = arabicName;
        this.englishName = englishName;
        this.flagImageUrl = flagImageUrl;
        this.locationType = locationType;
        this.slug = CatalogSlugGenerator.fromEnglishName(englishName);
    }

    public CountryEntity(String id, String arabicName, String englishName, String flagImageUrl,
                         LocationType locationType, String slug) {
        this.id = id;
        this.arabicName = arabicName;
        this.englishName = englishName;
        this.flagImageUrl = flagImageUrl;
        this.locationType = locationType;
        this.slug = slug;
    }

    @PrePersist
    void ensureSlugOnPersist() {
        if (slug == null || slug.isBlank()) {
            String base = CatalogSlugGenerator.fromEnglishName(englishName);
            if (base.isBlank()) {
                base = CatalogSlugGenerator.fromEnglishName(id);
            }
            this.slug = base.isBlank() ? "location" : base;
        }
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

    public String getSlug() {
        return slug;
    }

    /**
     * Assigns slug only when missing (set-once semantics for callers creating entities).
     */
    public void assignSlugIfAbsent(String candidateSlug) {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = candidateSlug;
        }
    }
}
