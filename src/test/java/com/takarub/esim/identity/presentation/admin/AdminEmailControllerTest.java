package com.takarub.esim.identity.presentation.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.email.SmtpConfig;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = AdminEmailController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
class AdminEmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmtpConfigService smtpConfigService;

    @MockBean
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder auditEventRecorder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSmtpConfigReturnsActiveConfig() throws Exception {
        SmtpConfig config = new SmtpConfig(
                1L, "smtp.gmail.com", 587, "user@gmail.com", "********",
                true, true, "from@gmail.com", true);
        when(smtpConfigService.getActiveConfig()).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/v1/admin/email/smtp-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("smtp.gmail.com"))
                .andExpect(jsonPath("$.port").value(587))
                .andExpect(jsonPath("$.username").value("user@gmail.com"))
                .andExpect(jsonPath("$.password").value("********"))
                .andExpect(jsonPath("$.fromEmail").value("from@gmail.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSmtpConfigReturnsNoContentWhenNoneConfigured() throws Exception {
        when(smtpConfigService.getActiveConfig()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/email/smtp-config"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getSmtpConfigRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/email/smtp-config"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSmtpConfigSavesAndReturns() throws Exception {
        SmtpConfig saved = new SmtpConfig(
                1L, "smtp.gmail.com", 587, "user@gmail.com", "********",
                true, true, "from@gmail.com", true);
        when(smtpConfigService.saveConfig(any())).thenReturn(saved);

        mockMvc.perform(put("/api/v1/admin/email/smtp-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "host": "smtp.gmail.com",
                                    "port": 587,
                                    "username": "user@gmail.com",
                                    "password": "app-password-123",
                                    "authEnabled": true,
                                    "tlsEnabled": true,
                                    "fromEmail": "from@gmail.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("smtp.gmail.com"))
                .andExpect(jsonPath("$.password").value("********"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSmtpConfigValidatesRequest() throws Exception {
        mockMvc.perform(put("/api/v1/admin/email/smtp-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "host": "",
                                    "port": null,
                                    "username": "",
                                    "fromEmail": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSmtpConfigRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/admin/email/smtp-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
