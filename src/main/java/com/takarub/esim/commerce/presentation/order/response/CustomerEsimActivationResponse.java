package com.takarub.esim.commerce.presentation.order.response;

/**
 * Customer eSIM activation response. Nullable fields follow domain reality (usable proof is QR
 * and/or SMDP+activationCode; iccid/pin/puk optional).
 */
public record CustomerEsimActivationResponse(
        String orderId,
        String iccid,
        String qrString,
        String smdpAddress,
        String activationCode,
        String pin,
        String puk) {
}
