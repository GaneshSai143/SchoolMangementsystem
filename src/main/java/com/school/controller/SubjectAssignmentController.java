package com.school.controller;

import com.school.dto.SubjectAssignmentRequestDTO;
import com.school.dto.SubjectAssignmentResponseDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.SubjectAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.school.dto.ErrorResponseDTO;
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
    @Operation(summary = "Create a new subject assignment", description = "Links a subject to a class and a teacher for a specific academic year and term. ADMIN can only assign for their school.")
    @RequestBody(description = "Details of the subject assignment", required = true, content = @Content(schema = @Schema(implementation = SubjectAssignmentRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Assignment created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectAssignmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, or e.g. teacher not in admin's school)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class, Subject, or Teacher not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SubjectAssignmentResponseDTO> createAssignment(@Valid @RequestBody SubjectAssignmentRequestDTO requestDTO) {
        SubjectAssignmentResponseDTO createdAssignment = subjectAssignmentService.createAssignment(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(createdAssignment, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subject assignment by ID", description = "All authenticated users can generally view subject assignments. Finer-grained access (e.g., teacher seeing only their assignments) can be handled if necessary but not explicitly implemented here.")
    @Parameter(name = "id", description = "ID of the subject assignment to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignment", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectAssignmentResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SubjectAssignmentResponseDTO> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentById(id));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all subject assignments for a specific class")
    @Parameter(name = "classId", description = "ID of the class", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignments", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectAssignmentResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<SubjectAssignmentResponseDTO>> getAssignmentsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentsByClassId(classId));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all subject assignments for a specific teacher (profile ID)")
    @Parameter(name = "teacherId", description = "ID of the teacher profile", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignments", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectAssignmentResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Teacher not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<SubjectAssignmentResponseDTO>> getAssignmentsByTeacherId(@PathVariable Long teacherId) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentsByTeacherId(teacherId));
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all subject assignments for a specific subject")
    @Parameter(name = "subjectId", description = "ID of the subject", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignments", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectAssignmentResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<SubjectAssignmentResponseDTO>> getAssignmentsBySubjectId(@PathVariable Long subjectId) {
        return ResponseEntity.ok(subjectAssignmentService.getAssignmentsBySubjectId(subjectId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update the status of a subject assignment (e.g., ACTIVE, COMPLETED)", description = "ADMIN can only update for assignments in their school.")
    @Parameter(name = "id", description = "ID of the subject assignment to update", required = true, in = ParameterIn.PATH)
    @RequestBody(description = "Payload containing the new status. Example: {\"status\": \"COMPLETED\"}", required = true, content = @Content(mediaType = "application/json", schema = @Schema(type="object", example = "{\"status\": \"COMPLETED\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment status updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectAssignmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g. missing status or invalid value)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
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
    @Operation(summary = "Delete a subject assignment", description = "ADMIN can only delete assignments in their school.")
    @Parameter(name = "id", description = "ID of the subject assignment to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Assignment deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        subjectAssignmentService.deleteAssignment(id, getCurrentlyLoggedInUser());
        return ResponseEntity.noContent().build();
    }
}
