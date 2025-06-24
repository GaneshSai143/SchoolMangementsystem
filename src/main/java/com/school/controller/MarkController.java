package com.school.controller;

import com.school.dto.EnterMarkRequestDTO;
import com.school.dto.MarkResponseDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.MarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody;
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

@RestController
@RequestMapping("/api/marks")
@RequiredArgsConstructor
@Tag(name = "Marks Management", description = "APIs for entering and viewing student marks")
public class MarkController {

    private final MarkService markService;
    private final UserRepository userRepository;

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + userEmail));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Enter marks for a student for a specific subject assignment.", description = "Requires TEACHER role. Teacher must be authorized for the student/subject assignment (e.g., class teacher or assigned subject teacher).")
    @SwaggerRequestBody(description = "Details of the marks to be entered", required = true, content = @Content(schema = @Schema(implementation = EnterMarkRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Marks entered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarkResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, student not in class of SA)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (teacher not authorized)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student or Subject Assignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MarkResponseDTO> enterMarks(@Valid @RequestBody EnterMarkRequestDTO requestDTO) {
        MarkResponseDTO savedMark = markService.enterMarks(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(savedMark, HttpStatus.CREATED);
    }

    @PutMapping("/{markId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an existing mark record.", description = "Teachers can update marks they recorded or for their class. Admins/SuperAdmins have broader access. Student/SubjectAssignment cannot be changed via this method.")
    @Parameter(name = "markId", description = "ID of the mark record to update", required = true, in = ParameterIn.PATH)
    @SwaggerRequestBody(description = "Updated mark details", required = true, content = @Content(schema = @Schema(implementation = EnterMarkRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mark record updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarkResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, cannot change student/SA)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Mark record not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MarkResponseDTO> updateMarks(@PathVariable Long markId, @Valid @RequestBody EnterMarkRequestDTO requestDTO) {
        MarkResponseDTO updatedMark = markService.updateMarks(markId, requestDTO, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(updatedMark);
    }

    @GetMapping("/{markId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a specific mark record by its ID.", description = "Access controlled by service layer (student, relevant teachers, admin).")
    @Parameter(name = "markId", description = "ID of the mark record to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved mark record", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarkResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Mark record not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MarkResponseDTO> getMarkById(@PathVariable Long markId) {
        return ResponseEntity.ok(markService.getMarkById(markId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/student/profile/{studentProfileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all marks for a student (by student profile ID).", description = "Access controlled by service layer.")
    @Parameter(name = "studentProfileId", description = "ID of the student profile", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved marks", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarkResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<MarkResponseDTO>> getMarksByStudentProfileId(@PathVariable Long studentProfileId) {
        return ResponseEntity.ok(markService.getMarksByStudentId(studentProfileId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/student/user/{studentUserId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all marks for a student (by main user ID).", description = "Access controlled by service layer.")
    @Parameter(name = "studentUserId", description = "User ID of the student", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved marks", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarkResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student user account or profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<MarkResponseDTO>> getMarksByStudentUserId(@PathVariable Long studentUserId) {
        return ResponseEntity.ok(markService.getMarksByStudentUserId(studentUserId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/subject-assignment/{saId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all marks for a specific subject assignment.", description = "Access controlled by service layer (e.g., teacher of SA, class teacher, admin).")
    @Parameter(name = "saId", description = "ID of the Subject Assignment", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved marks", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarkResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Subject Assignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<MarkResponseDTO>> getMarksBySubjectAssignmentId(@PathVariable Long saId) {
        return ResponseEntity.ok(markService.getMarksBySubjectAssignmentId(saId, getCurrentlyLoggedInUser()));
    }

    @DeleteMapping("/{markId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a mark record by its ID.", description = "Deletion access controlled by service layer (original recorder, class teacher, admin).")
    @Parameter(name = "markId", description = "ID of the mark record to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Mark record deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Mark record not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteMark(@PathVariable Long markId){
        markService.deleteMark(markId, getCurrentlyLoggedInUser());
        return ResponseEntity.noContent().build();
    }
}
