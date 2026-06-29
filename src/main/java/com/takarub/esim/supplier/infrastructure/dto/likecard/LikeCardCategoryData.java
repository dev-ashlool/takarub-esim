package com.takarub.esim.supplier.infrastructure.dto.likecard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single category entry inside the LikeCard YaHala categories {@code data} array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LikeCardCategoryData(
        @JsonProperty("categoryId") String categoryId,
        @JsonProperty("id") String id,
        @JsonProperty("categoryName") String categoryName) {
}
