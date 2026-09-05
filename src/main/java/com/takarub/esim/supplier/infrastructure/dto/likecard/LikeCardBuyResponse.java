package com.takarub.esim.supplier.infrastructure.dto.likecard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Flat top-level LikeCard YaHala {@code /online/yahala/buy} response envelope.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LikeCardBuyResponse(
        @JsonProperty("response") int response,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("iccid") String iccid,
        @JsonProperty("smdp_address") String smdpAddress,
        @JsonProperty("activation_code") String activationCode,
        @JsonProperty("pin") String pin,
        @JsonProperty("puk") String puk,
        @JsonProperty("qrString") String qrString,
        @JsonProperty("message") String message) {
}
