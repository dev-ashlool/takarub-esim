package com.takarub.esim.identity.presentation.customer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.identity.application.command.UpdateCustomerProfileCommand;
import com.takarub.esim.identity.application.query.GetUserByIdQuery;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.application.usecase.UpdateCustomerProfileUseCase;
import com.takarub.esim.identity.presentation.customer.request.UpdateCustomerProfileRequest;
import com.takarub.esim.identity.presentation.users.mapper.UserMapper;
import com.takarub.esim.identity.presentation.users.response.UserResponse;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Profile", description = "Authenticated customer My Account profile")
public class CustomerProfileController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateCustomerProfileUseCase updateCustomerProfileUseCase;
    private final UserMapper userMapper;
    private final SecurityContextProvider securityContextProvider;

    public CustomerProfileController(GetUserByIdUseCase getUserByIdUseCase,
                                     UpdateCustomerProfileUseCase updateCustomerProfileUseCase,
                                     UserMapper userMapper,
                                     SecurityContextProvider securityContextProvider) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.updateCustomerProfileUseCase = updateCustomerProfileUseCase;
        this.userMapper = userMapper;
        this.securityContextProvider = securityContextProvider;
    }

    @GetMapping("/me")
    @Operation(summary = "Return current customer info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<UserResponse> getMe() {
        String userId = requireUserId();
        return ResponseEntity.ok(userMapper.toResponse(
                getUserByIdUseCase.execute(new GetUserByIdQuery(userId))));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current customer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateCustomerProfileRequest request) {
        String userId = requireUserId();
        return ResponseEntity.ok(userMapper.toResponse(updateCustomerProfileUseCase.execute(
                new UpdateCustomerProfileCommand(
                        userId,
                        request.fullName(),
                        request.country(),
                        request.phoneNumber()))));
    }

    private String requireUserId() {
        return securityContextProvider.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
