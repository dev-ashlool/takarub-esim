# 05 — Identity Module (Deep Dive)

**Package:** `com.takarub.esim.identity`  
**Wiring:** `infrastructure.config.UseCaseConfig`, `SecurityConfig`, `NotificationConfig`

---

## Controllers (lifecycle: request-scoped Spring beans)

### `AuthenticationController`
- **Responsibility:** HTTP facade for auth lifecycle.
- **Depends on:** all auth use cases, `AuthenticationMapper`, `AccessTokenIssuer`, `AuditEventRecorder`, `SecurityContextProvider`, `GetSessionByIdUseCase`, `GetUserByIdUseCase`.
- **Risk if modified:** break login/register contracts; token shape changes break clients.

### `UserController`
- **Responsibility:** user lookup APIs.
- **Risk:** exposing PII; email lookup is ADMIN-only.

### `AdminEmailController`
- **Responsibility:** SMTP configuration CRUD via `SmtpConfigService` (no use-case layer).
- **Risk:** wrong encryption secrets brick stored passwords.

---

## Use cases — methods & logic

### `RegisterUserUseCase.execute(RegisterUserCommand)`
**When:** POST `/auth/register`  
**Params:** email, rawPassword  
**Returns:** `RegisterUserResult` (userId, verificationId, …)  
**Steps:**
1. Reject duplicate email (`DuplicateUserApplicationException`)
2. Hash password (`PasswordHasher`)
3. `User.register` with CUSTOMER + PENDING_VERIFICATION
4. `Verification.issue(EMAIL_VERIFICATION)` with TTL
5. Persist both in `TransactionRunner`
6. `NotificationSender.sendEmailVerification`

**Calls:** UserRepository, VerificationRepository, PasswordHasher, NotificationSender, IdGenerator, ClockProvider  
**Called by:** AuthenticationController

### `AuthenticateUserUseCase.execute`
**Steps:** find user → match password → `ensureCanAuthenticate` → `CreateSessionUseCase` → issue access JWT  
**Returns:** accessToken, refreshToken, sessionId, userId, expires…

### `CreateSessionUseCase.execute`
**Steps:** load user → revoke existing ACTIVE sessions → `Session.start` with TTL → save  
**Invariant:** one active session per user (application-enforced)

### `RefreshSessionUseCase.execute`
**Steps:** load session → `verifyRefreshToken` → `rotateRefreshToken` → save  
Controller then re-issues access JWT from user+session.

### `VerifyEmailUseCase.execute`
**Steps:** load verification by id/token → must be EMAIL_VERIFICATION → consume → `user.verifyEmail` → ACTIVE

### `RequestPasswordResetUseCase` / `ConfirmPasswordResetUseCase`
Issue/consume PASSWORD_RESET; confirm also `changePassword` + revoke sessions.

### `LogoutUseCase` / `RevokeSessionUseCase`
Set session REVOKED.

### Query use cases
`GetUserById`, `GetUserByEmail`, `GetSessionById` — load + map to views.

---

## Domain aggregates

### `User`
| Field | Type |
|-------|------|
| id | UserId (UUID) |
| email | EmailAddress |
| passwordHash | PasswordHash |
| status | UserStatus |
| roles | Set&lt;Role&gt; |

**Key methods:** `register`, `verifyEmail`, `changePassword`, `ensureCanAuthenticate`, lock/suspend/delete, role assign/revoke.  
**Lifecycle:** PENDING_VERIFICATION → ACTIVE → LOCKED|SUSPENDED|DELETED (see transition table in `UserStatus`).

### `Session`
| Field | Type |
|-------|------|
| id | SessionId |
| userId | UserId |
| refreshToken | RefreshToken |
| status | SessionStatus |
| deviceMetadata | DeviceMetadata |
| expiresAt | Instant |

**Key methods:** `start`, `rotateRefreshToken`, `verifyRefreshToken`, `revoke`, `expire`, `recordActivity`.

### `Verification`
| Field | Type |
|-------|------|
| id | VerificationId (= HTTP token) |
| type | EMAIL_VERIFICATION / PASSWORD_RESET |
| status | PENDING → CONSUMED/EXPIRED/CANCELLED |

**Key methods:** `issue`, `consume`, `cancel`, `expire`.

### Value objects
`UserId`, `EmailAddress`, `PasswordHash`, `SessionId`, `RefreshToken`, `DeviceMetadata`, `VerificationId` — validation in constructors; modification risk = auth breakage.

---

## Infrastructure highlights

| Class | Purpose | Inputs/Outputs | Risk |
|-------|---------|----------------|------|
| `JwtAccessTokenIssuer` | Build HS256 JWT | user/session/roles → compact JWT | secret/issuer mismatch |
| `JwtAccessTokenValidator` | Parse JWT | token → ValidatedJwtClaims | |
| `JwtAuthenticationFilter` | OncePerRequestFilter | Authorization header → SecurityContext | skip paths wrong → open APIs |
| `AuthenticatedSessionValidator` | Ensure sid still ACTIVE | sessionId | |
| `BCryptPasswordHasher` | hash/matches | raw ↔ hash | cost factor changes |
| `UserRepositoryAdapter` + mapper | User ↔ `UserJpaEntity` | | schema drift |
| `SessionRepositoryAdapter` | Session persistence | | refresh token plaintext |
| `VerificationRepositoryAdapter` | Verification persistence | | |
| `SmtpConfigService` | Cache JavaMailSender 5m | admin CRUD | |
| `SmtpConfigEncryptor` | AES text encryptor | password ↔ cipher | salt/password |
| `SmtpNotificationSender` / `LoggingNotificationSender` | mail vs log | | |
| `LoggingAuditEventRecorder` | SLF4J audit | login/logout events | not durable DB audit |
| `SpringTransactionRunner` | `@Transactional` wrapper | Runnable/Supplier | |

---

## Shared kernel (`identity.shared`)

Used across modules:

- Exceptions: `BaseException`, `ValidationException`, `UnauthorizedException`, …
- `IdGenerator` / `UuidIdGenerator`
- `ClockProvider` / `SystemClockProvider`
- `SecurityContextProvider` / `UserPrincipal`
- `AuditEvent` / `AuditEventRecorder`

**Why under identity:** historical — first module; other modules import it. Changing packages breaks imports project-wide.

---

## GlobalExceptionHandler

Maps exceptions to HTTP (see architecture).  
**Called by:** Spring MVC for any controller.  
**Risk:** swallowing stack traces in 500 (message generic) — intentional for clients; logs server-side.

---

## Tests (identity)

Cover domain reconstitution, use cases, JWT, filter, adapters, controllers, SMTP encryptor, GlobalExceptionHandler. See `src/test/java/com/takarub/esim/identity/**`.
