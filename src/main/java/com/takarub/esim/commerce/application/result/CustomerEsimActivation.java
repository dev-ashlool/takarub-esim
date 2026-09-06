package com.takarub.esim.commerce.application.result;

import com.takarub.esim.commerce.domain.order.OrderId;

/**
 * Customer-facing eSIM activation payload. Omits supplier identifiers and internal fulfillment
 * errors.
 */
public record CustomerEsimActivation(
        OrderId orderId,
        String iccid,
        String qrString,
        String smdpAddress,
        String activationCode,
        String pin,
        String puk) {
}
