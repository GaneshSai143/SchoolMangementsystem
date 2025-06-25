package com.school.controller;

import com.school.dto.UserDTO;
import com.school.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Corrected import
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.school.dto.ErrorResponseDTO; // Added for error responses
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // Added
import org.springframework.security.core.context.SecurityContextHolder; // Added
import org.springframework.web.bind.annotation.*;

import java.util.Map; // Added

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API for managing user accounts")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user details", description = "Retrieves the profile of the currently authenticated user, including preferences like theme.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user details", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized if user is not authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get user details by ID", description = "Requires ADMIN or SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the user to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user details", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #id == authentication.principal.id")
    @Operation(
        summary = "Update user details",
        description = "Allows ADMIN/SUPER_ADMIN to update any user, or a user to update their own profile. Password update is conditional.",
        requestBody = @RequestBody(description = "User details to update. Password field is optional; if provided, it will be updated.", required = true, content = @Content(schema = @Schema(implementation = UserDTO.class)))
    )
    @Parameter(name = "id", description = "ID of the user to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a user", description = "Requires SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the user to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully (deprecated, use 204)"), // Changed to 204 in spec
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update user role", description = "Requires SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the user whose role is to be updated", required = true, in = ParameterIn.PATH)
    @Parameter(name = "role", description = "New role for the user (e.g., ADMIN, TEACHER, STUDENT)", required = true, in = ParameterIn.QUERY)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User role updated successfully (deprecated, use 204)"),
            @ApiResponse(responseCode = "204", description = "User role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request (invalid role)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateUserRole(id, role);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Enable a user account", description = "Requires ADMIN or SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the user to enable", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User account enabled successfully (deprecated, use 204)"),
            @ApiResponse(responseCode = "204", description = "User account enabled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Disable a user account", description = "Requires ADMIN or SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the user to disable", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User account disabled successfully (deprecated, use 204)"),
            @ApiResponse(responseCode = "204", description = "User account disabled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/theme")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update preferred theme for the current user",
        description = "Accepts a JSON payload with a 'theme' key, e.g., {\"theme\": \"#RRGGBB\"} or {\"theme\": \"dark-mode\"}",
        requestBody = @RequestBody(description = "JSON payload containing the theme preference. Example: {\"theme\": \"dark\"}", required = true, content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"theme\": \"dark\"}")))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Theme updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., missing 'theme' key in payload)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDTO> updateUserTheme(@RequestBody Map<String, String> themePayload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        String theme = themePayload.get("theme");
        if (theme == null) { // Basic validation for payload
            throw new IllegalArgumentException("Payload must contain a 'theme' key with the theme value.");
        }
        UserDTO updatedUser = userService.updateUserTheme(userEmail, theme);
        return ResponseEntity.ok(updatedUser);
    }
}