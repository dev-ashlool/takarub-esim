package com.takarub.esim.commerce.application.port;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.domain.user.EmailAddress;

/**
 * Best-effort customer notification that an eSIM is ready. Implementations must not include
 * activation secrets (QR, activation code, SMDP, ICCID, PIN, PUK) or supplier internals.
 */
public interface EsimReadyCustomerNotifier {

    void notifyEsimReady(OrderId orderId, EmailAddress recipient);
}
