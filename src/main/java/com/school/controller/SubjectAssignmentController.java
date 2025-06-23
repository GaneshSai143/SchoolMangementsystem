package com.school.controller;

import com.school.dto.SubjectAssignmentRequestDTO;
import com.school.dto.SubjectAssignmentResponseDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.SubjectAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/subject-assignments")
@RequiredArgsConstructor
@Tag(name = "Subject Assignment Management", description = "APIs for assigning subjects to classes and teachers")
public class SubjectAssignmentController {

    private final SubjectAssignmentService subjectAssignmentService;
    private final UserRepository userRepository; // To get User entity for service calls

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + userEmail));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new subject assignment (link subject to class and teacher)")
    public ResponseEntity<SubjectAssignmentResponseDTO> createAssignment(@Valid @RequestBody SubjectAssignmentRequestDTO requestDTO) {
        SubjectAssignmentResponseDTO createdAssignment = subjectAssignmentService.createAssignment(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(createdAssignment, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subject assignment by ID")
    public ResponseEntity<SubjectAssignmentResponseDTO> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentById(id));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("isAuthenticated()") // Further checks for teacher/student role if needed in service
    @Operation(summary = "Get all subject assignments for a specific class")
    public ResponseEntity<List<SubjectAssignmentResponseDTO>> getAssignmentsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentsByClassId(classId));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("isAuthenticated()") // Further checks for teacher role if needed in service
    @Operation(summary = "Get all subject assignments for a specific teacher (profile ID)")
    public ResponseEntity<List<SubjectAssignmentResponseDTO>> getAssignmentsByTeacherId(@PathVariable Long teacherId) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentsByTeacherId(teacherId));
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all subject assignments for a specific subject")
    public ResponseEntity<List<SubjectAssignmentResponseDTO>> getAssignmentsBySubjectId(@PathVariable Long subjectId) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentsBySubjectId(subjectId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update the status of a subject assignment (e.g., ACTIVE, COMPLETED)")
    public ResponseEntity<SubjectAssignmentResponseDTO> updateAssignmentStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
         String status = payload.get("status");
         if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty in payload.");
         }
        SubjectAssignmentResponseDTO updatedAssignment = subjectAssignmentService.updateAssignmentStatus(id, status, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(updatedAssignment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a subject assignment")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        subjectAssignmentService.deleteAssignment(id, getCurrentlyLoggedInUser());
        return ResponseEntity.noContent().build();
    }
}
