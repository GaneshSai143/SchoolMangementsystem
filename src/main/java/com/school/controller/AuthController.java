package com.school.controller;

import com.school.dto.LoginRequest;
import com.school.dto.LoginResponse;
import com.school.dto.RegisterRequest;
import com.school.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.school.dto.ErrorResponseDTO; // Added for error responses
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API for user registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account. Default role is STUDENT.")
    @SwaggerRequestBody(description = "User registration details", required = true, content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully (deprecated, use 201 or 204 if no content)"),
            // Assuming current behavior is 200 OK with empty body. For new registrations, 201 Created might be more standard.
            // Or 204 No Content if truly nothing is returned and that's the final state.
            // Let's adjust to what makes most sense - 200 OK if it implies "action processed".
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, email already exists)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login an existing user", description = "Authenticates a user and returns an access token.")
    @SwaggerRequestBody(description = "User login credentials", required = true, content = @Content(schema = @Schema(implementation = LoginRequest.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (invalid credentials)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
} 