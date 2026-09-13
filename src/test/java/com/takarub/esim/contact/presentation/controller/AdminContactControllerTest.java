package com.takarub.esim.contact.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.contact.application.result.ContactMessageView;
import com.takarub.esim.contact.application.usecase.ChangeContactStatusUseCase;
import com.takarub.esim.contact.application.usecase.ListContactMessagesUseCase;
import com.takarub.esim.contact.domain.ContactStatus;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = AdminContactController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListContactMessagesUseCase listContactMessagesUseCase;
    @MockBean
    private ChangeContactStatusUseCase changeContactStatusUseCase;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void listReturnsMessages() throws Exception {
        Instant now = Instant.parse("2026-09-13T07:00:00Z");
        String id = UUID.randomUUID().toString();
        when(listContactMessagesUseCase.execute()).thenReturn(List.of(new ContactMessageView(
                id, "Ahmad", "user@example.com", null, "Help", ContactStatus.NEW, now, now)));

        mockMvc.perform(get("/api/v1/admin/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void changeStatusReturnsUpdatedMessage() throws Exception {
        Instant now = Instant.parse("2026-09-13T07:00:00Z");
        String id = UUID.randomUUID().toString();
        when(changeContactStatusUseCase.execute(eq(id), eq(ContactStatus.RESOLVED)))
                .thenReturn(new ContactMessageView(
                        id, "Ahmad", "user@example.com", null, "Help",
                        ContactStatus.RESOLVED, now, now));

        mockMvc.perform(patch("/api/v1/admin/contact/{id}/status", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
