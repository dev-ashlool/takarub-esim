/**
 * Identity module root.
 *
 * <p>A modular-monolith feature module following Clean Architecture with package-by-feature
 * layering. Layers (inner depends on nothing outward):
 * <ul>
 *   <li>{@code domain} &mdash; pure Java business model (no framework dependencies)</li>
 *   <li>{@code application} &mdash; use cases / orchestration over the domain</li>
 *   <li>{@code infrastructure} &mdash; technical adapters and implementations</li>
 *   <li>{@code presentation} &mdash; inbound adapters (REST controllers, advices)</li>
 *   <li>{@code shared} &mdash; cross-cutting abstractions reused by all layers</li>
 * </ul>
 */
package com.takarub.esim.identity;
