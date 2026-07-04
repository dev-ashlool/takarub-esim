package com.takarub.esim.supplier.infrastructure.dto.likecard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level LikeCard YaHala {@code /online/yahala/products} response envelope.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LikeCardProductResponse(
        @JsonProperty("status") int response,
        @JsonProperty("data") List<LikeCardProductData> data) {
}
