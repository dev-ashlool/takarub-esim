package com.takarub.esim.commerce.infrastructure.notification;

import com.takarub.esim.commerce.application.port.EsimReadyCustomerNotifier;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.domain.user.EmailAddress;

/**
 * No-op notifier used when eSIM email notification is disabled.
 */
public class NoOpEsimReadyCustomerNotifier implements EsimReadyCustomerNotifier {

    @Override
    public void notifyEsimReady(OrderId orderId, EmailAddress recipient) {
        // intentionally empty — feature disabled
    }
}
