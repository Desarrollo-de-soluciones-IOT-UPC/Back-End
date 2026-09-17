package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.clientportal.application.ClientAssistantService;
import com.emsafe.clientportal.interfaces.rest.dto.ChatReplyDto;
import com.emsafe.clientportal.interfaces.rest.dto.ChatRequest;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Asistente "Astra" (US10) — la clave de Gemini nunca sale del backend. */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientAssistantController {

    private final ClientAssistantService assistant;
    private final AuthenticatedClient client;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatReplyDto>> chat(
            HttpServletRequest request,
            @Valid @RequestBody ChatRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(assistant.chat(client.id(request), req)));
    }
}
