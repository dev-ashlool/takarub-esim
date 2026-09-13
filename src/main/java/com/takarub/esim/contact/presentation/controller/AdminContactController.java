package com.takarub.esim.contact.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.contact.application.usecase.ChangeContactStatusUseCase;
import com.takarub.esim.contact.application.usecase.ListContactMessagesUseCase;
import com.takarub.esim.contact.presentation.request.ChangeContactStatusRequest;
import com.takarub.esim.contact.presentation.response.ContactMessageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/contact")
@Tag(name = "Contact Administration", description = "Admin inbox and status updates for contact messages")
public class AdminContactController {

    private final ListContactMessagesUseCase listContactMessagesUseCase;
    private final ChangeContactStatusUseCase changeContactStatusUseCase;

    public AdminContactController(ListContactMessagesUseCase listContactMessagesUseCase,
                                  ChangeContactStatusUseCase changeContactStatusUseCase) {
        this.listContactMessagesUseCase = listContactMessagesUseCase;
        this.changeContactStatusUseCase = changeContactStatusUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List contact messages newest first")
    public ResponseEntity<List<ContactMessageResponse>> list() {
        return ResponseEntity.ok(listContactMessagesUseCase.execute().stream()
                .map(ContactMessageResponse::from)
                .toList());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change contact message status")
    public ResponseEntity<ContactMessageResponse> changeStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody ChangeContactStatusRequest request) {
        return ResponseEntity.ok(ContactMessageResponse.from(
                changeContactStatusUseCase.execute(id, request.status())));
    }
}
