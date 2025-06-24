package com.school.controller;

import com.school.dto.SubjectRequestDTO;
import com.school.dto.SubjectResponseDTO;
import com.school.service.SubjectService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject Management", description = "APIs for managing general academic subjects")
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new subject", description = "Accessible by SUPER_ADMIN.")
    @SwaggerRequestBody(description = "Details of the subject to be created", required = true, content = @Content(schema = @Schema(implementation = SubjectRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subject created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, subject name/code exists)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SubjectResponseDTO> createSubject(@Valid @RequestBody SubjectRequestDTO requestDTO) {
        SubjectResponseDTO createdSubject = subjectService.createSubject(requestDTO);
        return new ResponseEntity<>(createdSubject, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subject by ID", description = "All authenticated users can view subjects.")
    @Parameter(name = "id", description = "ID of the subject to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved subject", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SubjectResponseDTO> getSubjectById(@PathVariable Long id) {
        SubjectResponseDTO subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(subject);
    }

    @GetMapping("/code/{subjectCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subject by subject code", description = "All authenticated users can view subjects.")
    @Parameter(name = "subjectCode", description = "Code of the subject to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved subject", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SubjectResponseDTO> getSubjectByCode(@PathVariable String subjectCode) {
        SubjectResponseDTO subject = subjectService.getSubjectByCode(subjectCode);
        return ResponseEntity.ok(subject);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all subjects", description = "All authenticated users can view subjects.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of subjects", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<SubjectResponseDTO>> getAllSubjects() {
        List<SubjectResponseDTO> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update an existing subject", description = "Accessible by SUPER_ADMIN.")
    @Parameter(name = "id", description = "ID of the subject to update", required = true, in = ParameterIn.PATH)
    @SwaggerRequestBody(description = "Updated subject details", required = true, content = @Content(schema = @Schema(implementation = SubjectRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subject updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, subject name/code exists)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SubjectResponseDTO> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequestDTO requestDTO) {
        SubjectResponseDTO updatedSubject = subjectService.updateSubject(id, requestDTO);
        return ResponseEntity.ok(updatedSubject);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a subject by ID", description = "Accessible by SUPER_ADMIN. Deletion might be restricted if subject is in use (e.g., assigned to classes).")
    @Parameter(name = "id", description = "ID of the subject to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Subject deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g. subject in use)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
