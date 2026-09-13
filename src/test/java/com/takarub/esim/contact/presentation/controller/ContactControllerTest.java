package com.takarub.esim.contact.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.takarub.esim.contact.application.result.ContactMessageView;
import com.takarub.esim.contact.application.usecase.SubmitContactMessageUseCase;
import com.takarub.esim.contact.domain.ContactStatus;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = ContactController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubmitContactMessageUseCase submitContactMessageUseCase;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void submitReturnsCreated() throws Exception {
        Instant now = Instant.parse("2026-09-13T07:00:00Z");
        when(submitContactMessageUseCase.execute(any(), any(), any(), any()))
                .thenReturn(new ContactMessageView(
                        UUID.randomUUID().toString(),
                        "Ahmad",
                        "user@example.com",
                        "+962790000000",
                        "Need help",
                        ContactStatus.NEW,
                        now,
                        now));

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ahmad",
                                  "email": "user@example.com",
                                  "phoneNumber": "+962790000000",
                                  "message": "Need help"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void submitValidationFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/contact")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email",
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
