package com.takarub.esim.identity.presentation.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.identity.application.port.AccessTokenIssuer;
import com.takarub.esim.identity.application.result.AuthenticateUserResult;
import com.takarub.esim.identity.application.result.RegisterUserResult;
import com.takarub.esim.identity.application.usecase.AuthenticateUserUseCase;
import com.takarub.esim.identity.application.usecase.ConfirmPasswordResetUseCase;
import com.takarub.esim.identity.application.usecase.GetSessionByIdUseCase;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.application.usecase.RefreshSessionUseCase;
import com.takarub.esim.identity.application.usecase.RegisterUserUseCase;
import com.takarub.esim.identity.application.usecase.RequestPasswordResetUseCase;
import com.takarub.esim.identity.application.usecase.VerifyEmailUseCase;
import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.auth.mapper.AuthenticationMapper;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = AuthenticationController.class)
@Import({AuthenticationMapper.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;
    @MockBean
    private AuthenticateUserUseCase authenticateUserUseCase;
    @MockBean
    private RefreshSessionUseCase refreshSessionUseCase;
    @MockBean
    private VerifyEmailUseCase verifyEmailUseCase;
    @MockBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @MockBean
    private ConfirmPasswordResetUseCase confirmPasswordResetUseCase;
    @MockBean
    private GetSessionByIdUseCase getSessionByIdUseCase;
    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean
    private AccessTokenIssuer accessTokenIssuer;
    @MockBean
    private LoggingAuditEventRecorder auditEventRecorder;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerReturnsCreated() throws Exception {
        UserId userId = UserId.of(UUID.randomUUID());
        when(registerUserUseCase.execute(any())).thenReturn(new RegisterUserResult(
                userId, EmailAddress.of("user@example.com"), UserStatus.PENDING_VERIFICATION,
                VerificationId.of(UUID.randomUUID())));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.value().toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void loginReturnsAuthenticationResponse() throws Exception {
        UserId userId = UserId.of(UUID.randomUUID());
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        when(authenticateUserUseCase.execute(any())).thenReturn(new AuthenticateUserResult(
                userId, sessionId, "access-token", RefreshToken.of("refresh-token"),
                Instant.parse("2026-06-16T12:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123",
                                  "deviceName": "iPhone",
                                  "deviceType": "mobile",
                                  "ipAddress": "10.0.0.1",
                                  "userAgent": "agent/1.0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(auditEventRecorder).recordLoginSuccess(userId.value().toString(), null);
    }

    @Test
    void registerValidationFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
