package com.emsafe.auth.controller;

import com.emsafe.auth.dto.LoginRequest;
import com.emsafe.auth.dto.LoginResponse;
import com.emsafe.auth.dto.RefreshResponse;
import com.emsafe.auth.service.AuthService;
import com.emsafe.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse response = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(authHeader)));
    }
}
