/**
 * Security foundation &mdash; shared contracts only.
 *
 * <p>Provides a framework-agnostic view of the current principal
 * ({@link com.takarub.esim.identity.shared.security.UserPrincipal}) and a contract to access it
 * ({@link com.takarub.esim.identity.shared.security.SecurityContextProvider}).
 *
 * <p>Intentionally contains NO Spring Security configuration, JWT handling, filters or
 * authentication logic; those arrive in later tasks.
 */
package com.takarub.esim.identity.shared.security;
