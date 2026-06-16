package com.takarub.esim.identity.presentation.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.identity.application.usecase.GetUserByEmailUseCase;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.presentation.users.mapper.UserMapper;
import com.takarub.esim.identity.presentation.users.response.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetUserByEmailUseCase getUserByEmailUseCase;
    private final UserMapper mapper;

    public UserController(GetUserByIdUseCase getUserByIdUseCase,
                          GetUserByEmailUseCase getUserByEmailUseCase,
                          UserMapper mapper) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.getUserByEmailUseCase = getUserByEmailUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(mapper.toResponse(
                getUserByIdUseCase.execute(mapper.toQuery(id))));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getByEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(mapper.toResponse(
                getUserByEmailUseCase.execute(mapper.toQueryByEmail(email))));
    }
}
