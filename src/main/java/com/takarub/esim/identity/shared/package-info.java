/**
 * Identity <strong>shared</strong> layer &mdash; cross-cutting foundations.
 *
 * <p>Reusable abstractions and contracts consumed by every other layer:
 * <ul>
 *   <li>{@code constants} &mdash; shared, non-business constants</li>
 *   <li>{@code exception} &mdash; base exception hierarchy, error codes, unified error response</li>
 *   <li>{@code security} &mdash; framework-agnostic security contracts</li>
 *   <li>{@code audit} &mdash; audit contracts (no persistence)</li>
 *   <li>{@code time} &mdash; centralized time access ({@code ClockProvider})</li>
 *   <li>{@code id} &mdash; centralized id generation ({@code IdGenerator})</li>
 * </ul>
 */
package com.takarub.esim.identity.shared;
