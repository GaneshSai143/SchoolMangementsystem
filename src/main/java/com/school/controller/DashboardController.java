package com.school.controller;

import com.school.dto.StudentDashboardDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Get dashboard summary for the currently logged-in student.")
    public ResponseEntity<StudentDashboardDTO> getStudentDashboardSummary() {
        StudentDashboardDTO dashboardData = dashboardService.getStudentDashboard(getCurrentlyLoggedInUser());
        return ResponseEntity.ok(dashboardData);
    }
}
