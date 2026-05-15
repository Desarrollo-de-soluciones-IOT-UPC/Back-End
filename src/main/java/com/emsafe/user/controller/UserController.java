package com.emsafe.user.controller;

import com.emsafe.shared.dto.ApiResponse;
import com.emsafe.user.dto.ChangePasswordRequest;
import com.emsafe.user.dto.CreateUserRequest;
import com.emsafe.user.dto.UpdateUserRequest;
import com.emsafe.user.dto.UserDto;
import com.emsafe.user.entity.AppUser;
import com.emsafe.user.repository.UserRepository;
import com.emsafe.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAll(
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll(role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest req) {
        UserDto created = userService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Password updated", null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(Authentication auth) {
        AppUser user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new com.emsafe.shared.exception.ResourceNotFoundException("User", 0L));
        return ResponseEntity.ok(ApiResponse.ok(UserDto.from(user)));
    }
}
