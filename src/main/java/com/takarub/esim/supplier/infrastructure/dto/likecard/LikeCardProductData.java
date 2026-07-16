package com.takarub.esim.supplier.infrastructure.dto.likecard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single product entry inside the LikeCard YaHala products {@code data} array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LikeCardProductData(
        @JsonProperty("productId") String productId,
        @JsonProperty("countryCode") String countryCode,
        @JsonProperty("countryIso") String countryIso,
        @JsonProperty("productName") String productName,
        @JsonProperty("priceWithVat") String priceWithVat,
        @JsonProperty("price") String price,
        @JsonProperty("currency") String currency,
        @JsonProperty("productCurrency") String productCurrency,
        @JsonProperty("productData") String productData,
        @JsonProperty("productDataUnit") String productDataUnit,
        @JsonProperty("validity") String validity,
        @JsonProperty("validityDays") String validityDays) {
}
