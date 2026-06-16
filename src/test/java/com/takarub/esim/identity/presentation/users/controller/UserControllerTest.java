package com.takarub.esim.identity.presentation.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.application.usecase.GetUserByEmailUseCase;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.presentation.users.mapper.UserMapper;

@WebMvcTest(controllers = UserController.class)
@Import({UserMapper.class, GlobalExceptionHandler.class, SecurityConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean
    private GetUserByEmailUseCase getUserByEmailUseCase;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    void getByIdReturnsUserWhenAuthenticated() throws Exception {
        UserId userId = UserId.of(UUID.randomUUID());
        when(getUserByIdUseCase.execute(any())).thenReturn(new UserView(
                userId,
                EmailAddress.of("user@example.com"),
                UserStatus.ACTIVE,
                EnumSet.of(Role.CUSTOMER),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/v1/users/{id}", userId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByEmailAllowedForAdmin() throws Exception {
        when(getUserByEmailUseCase.execute(any())).thenReturn(new UserView(
                UserId.of(UUID.randomUUID()),
                EmailAddress.of("admin-lookup@example.com"),
                UserStatus.ACTIVE,
                EnumSet.of(Role.CUSTOMER),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/v1/users/email/{email}", "admin-lookup@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin-lookup@example.com"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getByEmailForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users/email/{email}", "user@example.com"))
                .andExpect(status().isForbidden());
    }
}
