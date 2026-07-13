# 08 — Exhaustive File Inventory

Every tracked project source/config/doc file under inspection (excluding `target/`, `.git/`, binaries).

For each path: **Purpose** (inferred from type + package role), **Typical callers**, **Calls**, **Risk if modified**.

Convention: `*Controller` called by HTTP; `*UseCase` called by controllers; `*Adapter` implements ports; `*Entity` used by Spring Data; `*Test` verifies sibling production type; `package-info` documents package only.

## Project root

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `.gitignore` | Supporting source | Module-internal | Local impact |
## docs/

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `docs/AUTH_FLOW.md` | Documentation | Humans | Docs drift |
| `docs/PULL_REQUEST_platform-catalog-sync-cache-smtp-audit.md` | Documentation | Humans | Docs drift |
| `docs/technical/01-system-architecture.md` | Documentation | Humans | Docs drift |
| `docs/technical/02-api-catalog.md` | Documentation | Humans | Docs drift |
| `docs/technical/03-database.md` | Documentation | Humans | Docs drift |
| `docs/technical/04-business-flows.md` | Documentation | Humans | Docs drift |
| `docs/technical/05-identity-module.md` | Documentation | Humans | Docs drift |
| `docs/technical/06-catalog-pricing-module.md` | Documentation | Humans | Docs drift |
| `docs/technical/07-supplier-module.md` | Documentation | Humans | Docs drift |
| `docs/technical/README.md` | Documentation | Humans | Docs drift |
## Project root

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `pom.xml` | Maven build & dependencies | Maven | Build/runtime classpath |
| `README.md` | Documentation | Humans | Docs drift |
## src/main/java — catalog

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/java/com/takarub/esim/catalog/application/command/UpdateExchangeRateCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/catalog/application/port/CatalogBrowsePort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/catalog/application/port/CatalogPackagePort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/catalog/application/query/BrowseCatalogQuery.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/catalog/application/query/GetPackageDetailsQuery.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/catalog/application/query/SearchPackagesQuery.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/catalog/application/result/CatalogPackageView.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/application/result/CountryView.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/application/result/PackageDetailsView.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/application/result/PagedResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/application/result/UpdateExchangeRateResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/application/usecase/BrowseCatalogUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/catalog/application/usecase/BrowseCountriesUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/catalog/application/usecase/PackageDetailsUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/catalog/application/usecase/SearchPackagesUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/catalog/application/usecase/UpdateExchangeRateUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/catalog/domain/exceptions/CurrencyExchangeException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/catalog/domain/exceptions/PackageNotFoundException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/catalog/domain/model/DataSize.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/catalog/domain/model/NormalizedCost.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/catalog/domain/model/Price.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/catalog/domain/model/ValidityPeriod.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/catalog/domain/port/ExchangeRatePort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/catalog/domain/service/CurrencyNormalizationService.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/cache/CatalogCacheInvalidator.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/config/CacheConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/catalog/infrastructure/config/CatalogUseCaseConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogBrowseAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogPackageAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogPackageEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogPackageJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/CountryEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/CountryJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/ExchangeRateAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/ExchangeRateEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/catalog/infrastructure/persistence/ExchangeRateJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/catalog/presentation/controller/AdminExchangeRateController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/catalog/presentation/controller/CatalogController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/catalog/presentation/exception/CatalogExceptionHandler.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/catalog/presentation/mapper/CatalogMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/catalog/presentation/request/UpdateExchangeRateRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/catalog/presentation/response/CatalogPackageResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/presentation/response/CountryResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/presentation/response/PackageDetailsResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/presentation/response/SearchPackagesResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/catalog/presentation/response/UpdateExchangeRateResponse.java` | Output DTO / read model | Use case → controller | API response shape |
## src/main/java — root

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/java/com/takarub/esim/EsimApplication.java` | Spring Boot entrypoint | JVM main | App won't start |
## src/main/java — identity

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/java/com/takarub/esim/identity/application/command/AuthenticateUserCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/AuthenticateUserDeviceMetadata.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/application/command/ConfirmPasswordResetCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/CreateSessionCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/LogoutCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/RefreshSessionCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/RegisterUserCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/RequestPasswordResetCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/RevokeSessionCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/command/VerifyEmailCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/exception/DuplicateUserApplicationException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/application/exception/IdentityApplicationErrorCode.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/application/exception/SessionNotFoundApplicationException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/application/exception/UserNotFoundApplicationException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/application/exception/VerificationNotFoundApplicationException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/application/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/application/port/AccessTokenIssuer.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/application/port/NotificationSender.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/application/port/PasswordHasher.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/application/port/TransactionRunner.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/application/query/GetSessionByIdQuery.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/query/GetUserByEmailQuery.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/query/GetUserByIdQuery.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/identity/application/result/AuthenticateUserResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/ConfirmPasswordResetResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/CreateSessionResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/LogoutResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/RefreshSessionResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/RegisterUserResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/RequestPasswordResetResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/RevokeSessionResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/SessionView.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/UserView.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/result/VerifyEmailResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/application/usecase/AuthenticateUserUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/ConfirmPasswordResetUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/CreateSessionUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/GetSessionByIdUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/GetUserByEmailUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/GetUserByIdUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/LogoutUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/RefreshSessionUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/RegisterUserUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/RequestPasswordResetUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/RevokeSessionUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/application/usecase/VerifyEmailUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/identity/domain/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/domain/session/DeviceMetadata.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/session/exception/InvalidRefreshTokenException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/session/exception/InvalidSessionStateTransitionException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/session/exception/SessionExpiredException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/session/exception/SessionRevokedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/session/RefreshToken.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/session/Session.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/session/SessionErrorCode.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/session/SessionId.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/session/SessionRepository.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/session/SessionStatus.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/shared/AggregateRoot.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/EmailAddress.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/exception/InvalidUserStateTransitionException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/exception/UserAlreadyActiveException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/exception/UserDeletedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/exception/UserLockedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/exception/UserNotVerifiedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/exception/UserSuspendedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/PasswordHash.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/Role.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/User.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/UserErrorCode.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/user/UserId.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/UserRepository.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/user/UserStatus.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/verification/exception/InvalidVerificationStateTransitionException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/verification/exception/VerificationCancelledException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/verification/exception/VerificationConsumedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/verification/exception/VerificationExpiredException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/verification/Verification.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/verification/VerificationErrorCode.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/domain/verification/VerificationId.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/verification/VerificationRepository.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/verification/VerificationStatus.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/domain/verification/VerificationType.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/identity/infrastructure/audit/LoggingAuditEventRecorder.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/config/IdentityProperties.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/config/JwtProperties.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/config/MailProperties.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/config/NotificationConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/config/SecurityConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/config/UseCaseConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/email/NoActiveSmtpConfigException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/infrastructure/email/SmtpConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/infrastructure/email/SmtpConfigEncryptor.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/email/SmtpConfigEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/identity/infrastructure/email/SmtpConfigJpaRepository.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/email/SmtpConfigService.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/jwt/JwtAccessTokenIssuer.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/jwt/JwtAccessTokenValidator.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/jwt/JwtClaimsNames.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/jwt/ValidatedJwtClaims.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/notification/LoggingNotificationSender.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/notification/SmtpNotificationSender.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/adapter/SessionRepositoryAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/adapter/UserRepositoryAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/adapter/VerificationRepositoryAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/entity/DeviceMetadataEmbeddable.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/entity/SessionJpaEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/entity/UserJpaEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/entity/VerificationJpaEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/mapper/SessionPersistenceMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/mapper/UserPersistenceMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/mapper/VerificationPersistenceMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/repository/SessionJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/repository/UserJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/repository/VerificationJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/identity/infrastructure/persistence/SpringTransactionRunner.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/security/AuthenticatedSessionValidator.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/security/BCryptPasswordHasher.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/security/JwtAuthenticationFilter.java` | Servlet filter (security) | Security filter chain | Auth bypass or lockout |
| `src/main/java/com/takarub/esim/identity/infrastructure/security/SpringSecurityContextProvider.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/infrastructure/security/UserPrincipalAuthenticationToken.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/identity/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/presentation/admin/AdminEmailController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/identity/presentation/admin/SmtpConfigResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/presentation/admin/UpdateSmtpConfigRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/controller/AuthenticationController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/identity/presentation/auth/mapper/AuthenticationMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/identity/presentation/auth/request/ForgotPasswordRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/request/LoginRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/request/RefreshTokenRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/request/RegisterUserRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/request/ResetPasswordRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/request/VerifyEmailRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/identity/presentation/auth/response/AuthenticationResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/presentation/auth/response/RegisterUserResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/presentation/auth/response/ResetPasswordResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/presentation/auth/response/VerifyEmailResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/presentation/exception/GlobalExceptionHandler.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/presentation/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/presentation/shared/ErrorResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/presentation/shared/OpenApiConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/identity/presentation/users/controller/UserController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/identity/presentation/users/mapper/UserMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/identity/presentation/users/response/UserResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/shared/audit/AuditEvent.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/audit/AuditEventRecorder.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/audit/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/constants/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/constants/SharedConstants.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/exception/BaseException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/BusinessException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/ConflictException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/ErrorCode.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/ErrorResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/identity/shared/exception/FieldViolation.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/exception/ForbiddenException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/exception/ResourceNotFoundException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/SharedErrorCode.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/UnauthorizedException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/exception/ValidationException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/identity/shared/id/IdGenerator.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/id/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/id/UuidIdGenerator.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/security/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/security/SecurityContextProvider.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/security/UserPrincipal.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/time/ClockProvider.java` | Supporting source | Module-internal | Local impact |
| `src/main/java/com/takarub/esim/identity/shared/time/package-info.java` | Package Javadoc only — no runtime | None | Docs only |
| `src/main/java/com/takarub/esim/identity/shared/time/SystemClockProvider.java` | Supporting source | Module-internal | Local impact |
## src/main/java — pricing

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/java/com/takarub/esim/pricing/application/command/UpsertGlobalMarkupCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/pricing/application/command/UpsertPackagePricingCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/pricing/application/result/PricingConfigView.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/pricing/application/usecase/DeletePackagePricingUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/pricing/application/usecase/GetPricingConfigUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/pricing/application/usecase/UpsertGlobalMarkupUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/pricing/application/usecase/UpsertPackagePricingUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/pricing/domain/model/PricingRule.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/pricing/domain/model/PricingRuleType.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/pricing/domain/model/PricingScope.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/pricing/domain/model/SellPrice.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/pricing/domain/port/CheapestNormalizedCostPort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/pricing/domain/port/PricingRulePort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/pricing/domain/service/SellPriceResolver.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/pricing/infrastructure/config/PricingUseCaseConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/pricing/infrastructure/persistence/CheapestNormalizedCostAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/pricing/infrastructure/persistence/PricingRuleAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/pricing/infrastructure/persistence/PricingRuleEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/pricing/infrastructure/persistence/PricingRuleJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/pricing/presentation/controller/AdminPricingController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/pricing/presentation/request/UpsertGlobalMarkupRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/pricing/presentation/request/UpsertPackagePricingRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/pricing/presentation/response/PricingConfigResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/pricing/presentation/response/PricingRuleResponse.java` | Output DTO / read model | Use case → controller | API response shape |
## src/main/java — supplier

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/java/com/takarub/esim/supplier/application/command/SyncSupplierCatalogCommand.java` | Application input DTO | Controller → use case | Low if fields additive |
| `src/main/java/com/takarub/esim/supplier/application/port/SupplierCredentialsPort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/supplier/application/port/SupplierLikeCardProductLogPort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/supplier/application/port/SupplierPackageMappingPort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/supplier/application/port/SupplierSyncAuditLogPort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/supplier/application/port/SyncJobPort.java` | Outbound/inbound port interface | Use cases → adapters | Contract change forces all impls |
| `src/main/java/com/takarub/esim/supplier/application/result/SyncSupplierCatalogResult.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/supplier/application/usecases/SyncSupplierCatalogUseCase.java` | Application use case orchestration | Controller → ports/domain | Business flow break |
| `src/main/java/com/takarub/esim/supplier/domain/exceptions/SupplierApiException.java` | Typed error / error code | Thrown by domain/app; handled by advice | HTTP status mapping |
| `src/main/java/com/takarub/esim/supplier/domain/model/CountryInfo.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/DataUnit.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/LocationClassifier.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/LocationType.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/RawSupplierProduct.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/RegionNormalizer.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/SupplierSyncAuditLog.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/SupplierSyncAuditStatus.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/SupplierType.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/model/SyncJob.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/domain/port/SupplierCatalogClient.java` | Domain type (aggregate/VO/service/enum) | Use cases / other domain | Invariant break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/adapters/likecard/LikeCardCurrencyTranslator.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/adapters/likecard/LikeCardProductMapper.java` | Maps between layers/DTOs | Controllers/adapters | Field mapping bugs |
| `src/main/java/com/takarub/esim/supplier/infrastructure/adapters/likecard/LikeCardSupplierAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/config/SchedulingConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/supplier/infrastructure/config/SupplierUseCaseConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/main/java/com/takarub/esim/supplier/infrastructure/dto/likecard/LikeCardCategoriesResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/supplier/infrastructure/dto/likecard/LikeCardCategoryData.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/dto/likecard/LikeCardCountriesResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/supplier/infrastructure/dto/likecard/LikeCardCountryData.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/dto/likecard/LikeCardProductData.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/dto/likecard/LikeCardProductResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/CatalogPackageEntityResolver.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierCredentialsPortAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierCredentialsProvider.java` | Infrastructure implementation | Ports/config | Integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierLikeCardProductEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierLikeCardProductJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierLikeCardProductLogAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierPackageMappingAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierPackageMappingEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierPackageMappingJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierSyncAuditLogAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierSyncAuditLogEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierSyncAuditLogJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SyncJobAdapter.java` | Hexagonal adapter (port impl) | Use cases via port | Persistence/integration break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SyncJobEntity.java` | JPA persistence model | Repositories/adapters | Schema/mapping mismatch |
| `src/main/java/com/takarub/esim/supplier/infrastructure/persistence/SyncJobJpaRepository.java` | Spring Data repository | Adapters | Query/schema break |
| `src/main/java/com/takarub/esim/supplier/infrastructure/scheduler/CatalogSyncScheduler.java` | Scheduled / dynamic job runner | Spring scheduling | Duplicate or missed syncs |
| `src/main/java/com/takarub/esim/supplier/infrastructure/scheduling/SyncJobSchedulerService.java` | Scheduled / dynamic job runner | Spring scheduling | Duplicate or missed syncs |
| `src/main/java/com/takarub/esim/supplier/presentation/controller/AdminSupplierController.java` | REST controller — HTTP entry | Spring MVC → use cases/services | API contract break |
| `src/main/java/com/takarub/esim/supplier/presentation/request/CreateSyncJobRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/supplier/presentation/request/UpdateSyncJobRequest.java` | HTTP request body/params DTO | Controller binding | API validation contract |
| `src/main/java/com/takarub/esim/supplier/presentation/response/SyncAuditLogPageResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/supplier/presentation/response/SyncAuditLogResponse.java` | Output DTO / read model | Use case → controller | API response shape |
| `src/main/java/com/takarub/esim/supplier/presentation/response/SyncJobResponse.java` | Output DTO / read model | Use case → controller | API response shape |
## src/main/resources

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/resources/application.yml` | Spring configuration YAML | Boot Environment | Runtime misconfig |
| `src/main/resources/application-local.yml` | Spring configuration YAML | Boot Environment | Runtime misconfig |
| `src/main/resources/application-local.yml.example` | Spring configuration YAML | Boot Environment | Runtime misconfig |
## Flyway migrations

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/main/resources/db/migration/V1__create_users.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V10__region_normalization.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V11__create_sync_jobs.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V12__create_email_smtp_config.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V13__currency_normalization.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V14__create_pricing_rules.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V2__create_user_roles.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V3__create_sessions.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V4__create_verifications.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V5__create_supplier_credentials.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V6__create_catalog_and_supplier_schema.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V7__create_supplier_likecard_products.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V8__widen_country_iso_columns.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
| `src/main/resources/db/migration/V9__create_supplier_sync_audit_logs.sql` | Flyway migration | Flyway on startup | Irreversible schema drift |
## src/test — catalog

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/test/java/com/takarub/esim/catalog/application/usecase/BrowseCatalogUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/application/usecase/PackageDetailsUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/application/usecase/SearchPackagesUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/application/usecase/UpdateExchangeRateUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/domain/model/DataSizeTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/domain/model/NormalizedCostTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/domain/model/PriceTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/domain/model/ValidityPeriodTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/domain/service/CurrencyNormalizationServiceTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/infrastructure/config/TestCacheConfig.java` | Spring configuration / @ConfigurationProperties | Container wiring | Bean wiring / startup |
| `src/test/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogBrowseAdapterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogPackageAdapterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/infrastructure/persistence/CatalogPersistenceMappingTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/presentation/controller/AdminExchangeRateControllerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/presentation/controller/CatalogControllerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/catalog/presentation/exception/CatalogExceptionHandlerTest.java` | Automated test | Surefire | False confidence if weakened |
## src/test — identity

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/test/java/com/takarub/esim/identity/application/usecase/AuthenticateUserUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/application/usecase/LogoutUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/domain/session/SessionReconstituteTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/domain/user/UserReconstituteTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/domain/verification/VerificationReconstituteTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/email/SmtpConfigEncryptorTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/email/SmtpConfigServiceTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/IdentityWiringTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/jwt/JwtAccessTokenIssuerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/jwt/JwtAccessTokenValidatorTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/notification/LoggingNotificationSenderTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/notification/SmtpNotificationSenderTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/persistence/adapter/SessionRepositoryAdapterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/persistence/adapter/UserRepositoryAdapterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/persistence/adapter/VerificationRepositoryAdapterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/persistence/mapper/SessionPersistenceMapperTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/persistence/mapper/UserPersistenceMapperTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/persistence/mapper/VerificationPersistenceMapperTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/security/BCryptPasswordHasherTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/security/JwtAuthenticationFilterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/infrastructure/security/SpringSecurityContextProviderTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/presentation/admin/AdminEmailControllerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/presentation/auth/controller/AuthenticationControllerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/presentation/exception/GlobalExceptionHandlerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/presentation/users/controller/UserControllerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/identity/shared/SharedLayerSmokeTest.java` | Automated test | Surefire | False confidence if weakened |
## src/test — pricing

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/test/java/com/takarub/esim/pricing/application/usecase/UpsertGlobalMarkupUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/pricing/domain/service/SellPriceResolverTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/pricing/presentation/controller/AdminPricingControllerTest.java` | Automated test | Surefire | False confidence if weakened |
## src/test — supplier

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/test/java/com/takarub/esim/supplier/application/usecases/SyncSupplierCatalogUseCaseTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/domain/model/LocationClassifierTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/domain/model/RawSupplierProductTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/domain/model/RegionNormalizerTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/domain/model/SyncJobTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/infrastructure/adapters/likecard/LikeCardCurrencyTranslatorTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/infrastructure/adapters/likecard/LikeCardProductMapperTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/infrastructure/adapters/likecard/LikeCardSupplierAdapterTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierCredentialsProviderTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/infrastructure/persistence/SupplierPackageMappingAdapterRecalculationTest.java` | Automated test | Surefire | False confidence if weakened |
| `src/test/java/com/takarub/esim/supplier/presentation/controller/AdminSupplierControllerTest.java` | Automated test | Surefire | False confidence if weakened |
## src/test — other

| File | Purpose | Callers / Called | Risk |
|------|---------|------------------|------|
| `src/test/resources/application.yml` | Spring configuration YAML | Boot Environment | Runtime misconfig |

## Notes on `unimportant` files

- `package-info.java`: documentation markers for Clean Architecture packages — safe to ignore at runtime.
- `*_file_list.txt`: generated helper for this inventory (can delete).
- `mvnw` / `.mvn`: Maven wrapper — required for reproducible builds without global Maven.
- Example YAML: template only; real secrets go in gitignored `application-local.yml`.

## Cross-reference

For method-level logic of critical classes, see modules 05–07 and flows 04. For HTTP contracts see 02. For schema see 03.
