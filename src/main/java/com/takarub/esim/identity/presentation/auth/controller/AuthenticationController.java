package com.takarub.esim.identity.presentation.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.identity.application.port.AccessTokenIssuer;
import com.takarub.esim.identity.application.query.GetSessionByIdQuery;
import com.takarub.esim.identity.application.query.GetUserByIdQuery;
import com.takarub.esim.identity.application.result.RefreshSessionResult;
import com.takarub.esim.identity.application.result.SessionView;
import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.application.usecase.AuthenticateUserUseCase;
import com.takarub.esim.identity.application.usecase.ConfirmPasswordResetUseCase;
import com.takarub.esim.identity.application.usecase.GetSessionByIdUseCase;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.application.usecase.RefreshSessionUseCase;
import com.takarub.esim.identity.application.usecase.RegisterUserUseCase;
import com.takarub.esim.identity.application.usecase.RequestPasswordResetUseCase;
import com.takarub.esim.identity.application.usecase.VerifyEmailUseCase;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.presentation.auth.mapper.AuthenticationMapper;
import com.takarub.esim.identity.presentation.auth.request.ForgotPasswordRequest;
import com.takarub.esim.identity.presentation.auth.request.LoginRequest;
import com.takarub.esim.identity.presentation.auth.request.RefreshTokenRequest;
import com.takarub.esim.identity.presentation.auth.request.RegisterUserRequest;
import com.takarub.esim.identity.presentation.auth.request.ResetPasswordRequest;
import com.takarub.esim.identity.presentation.auth.request.VerifyEmailRequest;
import com.takarub.esim.identity.presentation.auth.response.AuthenticationResponse;
import com.takarub.esim.identity.presentation.auth.response.RegisterUserResponse;
import com.takarub.esim.identity.presentation.auth.response.ResetPasswordResponse;
import com.takarub.esim.identity.presentation.auth.response.VerifyEmailResponse;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ConfirmPasswordResetUseCase confirmPasswordResetUseCase;
    private final GetSessionByIdUseCase getSessionByIdUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthenticationMapper mapper;
    private final LoggingAuditEventRecorder auditEventRecorder;

    public AuthenticationController(RegisterUserUseCase registerUserUseCase,
                                  AuthenticateUserUseCase authenticateUserUseCase,
                                  RefreshSessionUseCase refreshSessionUseCase,
                                  VerifyEmailUseCase verifyEmailUseCase,
                                  RequestPasswordResetUseCase requestPasswordResetUseCase,
                                  ConfirmPasswordResetUseCase confirmPasswordResetUseCase,
                                  GetSessionByIdUseCase getSessionByIdUseCase,
                                  GetUserByIdUseCase getUserByIdUseCase,
                                  AccessTokenIssuer accessTokenIssuer,
                                  AuthenticationMapper mapper,
                                  LoggingAuditEventRecorder auditEventRecorder) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.confirmPasswordResetUseCase = confirmPasswordResetUseCase;
        this.getSessionByIdUseCase = getSessionByIdUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.accessTokenIssuer = accessTokenIssuer;
        this.mapper = mapper;
        this.auditEventRecorder = auditEventRecorder;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisterUserResponse response = mapper.toResponse(
                registerUserUseCase.execute(mapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthenticationResponse response = mapper.toResponse(
                    authenticateUserUseCase.execute(mapper.toCommand(request)));
            auditEventRecorder.recordLoginSuccess(response.userId(), null);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException ex) {
            auditEventRecorder.recordLoginFailure(request.email(), null);
            throw ex;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshSessionResult refreshed = refreshSessionUseCase.execute(mapper.toCommand(request));
        SessionView session = getSessionByIdUseCase.execute(
                new GetSessionByIdQuery(refreshed.sessionId().value().toString()));
        UserView user = getUserByIdUseCase.execute(
                new GetUserByIdQuery(session.userId().value().toString()));
        String accessToken = accessTokenIssuer.issueAccessToken(
                user.id(), refreshed.sessionId(), user.roles(), user.email());
        return ResponseEntity.ok(mapper.toResponse(refreshed, accessToken, user.id().value().toString()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(mapper.toResponse(
                verifyEmailUseCase.execute(mapper.toCommand(request))));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetUseCase.execute(mapper.toCommand(request));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = mapper.toResponse(
                confirmPasswordResetUseCase.execute(mapper.toCommand(request)));
        return ResponseEntity.ok(response);
    }
}
