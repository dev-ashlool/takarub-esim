package com.takarub.esim.identity.presentation.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.application.usecase.UpdateCustomerProfileUseCase;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.presentation.users.mapper.UserMapper;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

@WebMvcTest(controllers = CustomerProfileController.class)
@Import({UserMapper.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CustomerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean
    private UpdateCustomerProfileUseCase updateCustomerProfileUseCase;
    @MockBean
    private SecurityContextProvider securityContextProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMeReturnsProfile() throws Exception {
        UserId userId = UserId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId.value().toString()));
        when(getUserByIdUseCase.execute(any())).thenReturn(profile(userId));

        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Ahmad Ali"))
                .andExpect(jsonPath("$.country").value("JO"))
                .andExpect(jsonPath("$.phoneNumber").value("+962790000000"));
    }

    @Test
    void updateMeReturnsUpdatedProfile() throws Exception {
        UserId userId = UserId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId.value().toString()));
        when(updateCustomerProfileUseCase.execute(any())).thenReturn(profile(userId));

        mockMvc.perform(put("/api/v1/customers/me")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ahmad Ali",
                                  "country": "JO",
                                  "phoneNumber": "+962790000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("JO"));
    }

    @Test
    void getMeReturnsUnauthorizedWhenMissingSession() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    private static UserView profile(UserId userId) {
        return new UserView(
                userId,
                EmailAddress.of("user@example.com"),
                UserStatus.ACTIVE,
                EnumSet.of(Role.CUSTOMER),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-09-13T07:00:00Z"),
                "Ahmad Ali",
                "JO",
                "+962790000000");
    }
}
