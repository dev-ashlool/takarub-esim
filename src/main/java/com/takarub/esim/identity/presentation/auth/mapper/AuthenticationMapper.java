package com.takarub.esim.identity.presentation.auth.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.application.command.AuthenticateUserCommand;
import com.takarub.esim.identity.application.command.AuthenticateUserDeviceMetadata;
import com.takarub.esim.identity.application.command.ConfirmPasswordResetCommand;
import com.takarub.esim.identity.application.command.RefreshSessionCommand;
import com.takarub.esim.identity.application.command.RegisterUserCommand;
import com.takarub.esim.identity.application.command.RequestPasswordResetCommand;
import com.takarub.esim.identity.application.command.VerifyEmailCommand;
import com.takarub.esim.identity.application.result.AuthenticateUserResult;
import com.takarub.esim.identity.application.result.ConfirmPasswordResetResult;
import com.takarub.esim.identity.application.result.RefreshSessionResult;
import com.takarub.esim.identity.application.result.RegisterUserResult;
import com.takarub.esim.identity.application.result.VerifyEmailResult;
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

@Component
public class AuthenticationMapper {

    public RegisterUserCommand toCommand(RegisterUserRequest request) {
        return new RegisterUserCommand(request.email(), request.password());
    }

    public AuthenticateUserCommand toCommand(LoginRequest request) {
        return new AuthenticateUserCommand(
                request.email(),
                request.password(),
                new AuthenticateUserDeviceMetadata(
                        request.deviceName(),
                        request.deviceType(),
                        request.ipAddress(),
                        request.userAgent()));
    }

    public RefreshSessionCommand toCommand(RefreshTokenRequest request) {
        return new RefreshSessionCommand(request.sessionId(), request.refreshToken());
    }

    public VerifyEmailCommand toCommand(VerifyEmailRequest request) {
        return new VerifyEmailCommand(request.token());
    }

    public RequestPasswordResetCommand toCommand(ForgotPasswordRequest request) {
        return new RequestPasswordResetCommand(request.email());
    }

    public ConfirmPasswordResetCommand toCommand(ResetPasswordRequest request) {
        return new ConfirmPasswordResetCommand(request.token(), request.newPassword());
    }

    public RegisterUserResponse toResponse(RegisterUserResult result) {
        return new RegisterUserResponse(
                result.userId().value().toString(),
                result.email().value(),
                result.status().name());
    }

    public AuthenticationResponse toResponse(AuthenticateUserResult result) {
        return new AuthenticationResponse(
                result.userId().value().toString(),
                result.sessionId().value().toString(),
                result.accessToken(),
                result.refreshToken().value(),
                result.expiresAt());
    }

    public AuthenticationResponse toResponse(RefreshSessionResult session, String accessToken,
                                             String userId) {
        return new AuthenticationResponse(
                userId,
                session.sessionId().value().toString(),
                accessToken,
                session.refreshToken().value(),
                session.expiresAt());
    }

    public VerifyEmailResponse toResponse(VerifyEmailResult result) {
        return new VerifyEmailResponse(
                result.userId().value().toString(),
                result.status().name());
    }

    public ResetPasswordResponse toResponse(ConfirmPasswordResetResult result) {
        return new ResetPasswordResponse(
                result.userId().value().toString(),
                result.revokedSessionCount());
    }
}
