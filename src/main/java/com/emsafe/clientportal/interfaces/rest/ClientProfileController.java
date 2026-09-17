package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.clientportal.application.ClientProfileService;
import com.emsafe.clientportal.interfaces.rest.dto.ClientProfileDto;
import com.emsafe.clientportal.interfaces.rest.dto.DeleteAccountRequest;
import com.emsafe.clientportal.interfaces.rest.dto.UpdateClientProfileRequest;
import com.emsafe.iam.interfaces.rest.dto.ChangePasswordRequest;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cuenta del cliente autenticado. Rutas intactas (regla R1). */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService profiles;
    private final AuthenticatedClient client;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ClientProfileDto>> getProfile(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(profiles.getProfile(client.id(request))));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ClientProfileDto>> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateClientProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated",
                profiles.updateProfile(client.id(request), req)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest req) {
        profiles.changePassword(client.id(request), req);
        return ResponseEntity.ok(ApiResponse.ok("Password updated", null));
    }

    /** Baja voluntaria de la propia cuenta, confirmada con la contraseña. */
    @PostMapping("/account/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            HttpServletRequest request,
            @RequestBody DeleteAccountRequest req) {
        profiles.deleteAccount(client.id(request), req.password());
        return ResponseEntity.ok(ApiResponse.ok("Account deleted", null));
    }
}
