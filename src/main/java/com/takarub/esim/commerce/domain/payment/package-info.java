/**
 * Commerce payment domain: one {@link com.takarub.esim.commerce.domain.payment.PaymentAttempt}
 * models a single payment attempt for an {@link com.takarub.esim.commerce.domain.order.Order}.
 *
 * <p>An Order may have multiple attempts over time. A terminal failed attempt is never reused;
 * retry creates a new attempt. Provider-neutral external correlation identifiers may be attached
 * while the attempt is initiated. Provider integration, verification, and Order transitions are
 * outside this Domain.
 */
package com.takarub.esim.commerce.domain.payment;
