package com.takarub.esim.contact.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.contact.application.usecase.SubmitContactMessageUseCase;
import com.takarub.esim.contact.presentation.request.SubmitContactRequest;
import com.takarub.esim.contact.presentation.response.ContactMessageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact Us", description = "Public contact form for clients")
public class ContactController {

    private final SubmitContactMessageUseCase submitContactMessageUseCase;

    public ContactController(SubmitContactMessageUseCase submitContactMessageUseCase) {
        this.submitContactMessageUseCase = submitContactMessageUseCase;
    }

    @PostMapping
    @Operation(summary = "Submit a contact us form")
    @ApiResponse(responseCode = "201", description = "Message stored")
    public ResponseEntity<ContactMessageResponse> submit(@Valid @RequestBody SubmitContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactMessageResponse.from(
                submitContactMessageUseCase.execute(
                        request.name(),
                        request.email(),
                        request.phoneNumber(),
                        request.message())));
    }
}
