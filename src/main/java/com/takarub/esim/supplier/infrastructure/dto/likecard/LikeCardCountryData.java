package com.takarub.esim.supplier.infrastructure.dto.likecard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single country entry inside the LikeCard YaHala countries {@code data} array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LikeCardCountryData(
        @JsonProperty("countryIso") String countryIso,
        @JsonProperty("countryCode") String countryCode,
        @JsonProperty("countryName") String countryName,
        @JsonProperty("countryImage") String countryImage) {
}
