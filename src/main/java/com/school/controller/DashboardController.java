package com.school.controller;

import com.school.dto.StudentDashboardDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.school.dto.ErrorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Student Dashboard", description = "APIs for student dashboard information")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + userEmail));
    }

    @GetMapping("/student/summary")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get dashboard summary for the currently logged-in student.", description = "Provides a summary of pending tasks and performance for the authenticated student.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student dashboard data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDashboardDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (user not authenticated or not a STUDENT)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user does not have STUDENT role)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student profile not found for the user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<StudentDashboardDTO> getStudentDashboardSummary() {
        StudentDashboardDTO dashboardData = dashboardService.getStudentDashboard(getCurrentlyLoggedInUser());
        return ResponseEntity.ok(dashboardData);
    }
}
