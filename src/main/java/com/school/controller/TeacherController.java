package com.school.controller;

import com.school.dto.CreateTeacherRequestDTO;
import com.school.dto.TeacherDTO;
import com.school.dto.UpdateTeacherRequestDTO;
import com.school.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Corrected import
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
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Management", description = "APIs for managing teacher profiles")
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Create a new teacher profile",
        description = "Links an existing user with TEACHER role. Requires ADMIN or SUPER_ADMIN role.",
        requestBody = @RequestBody(description = "Details of the teacher to be created", required = true, content = @Content(schema = @Schema(implementation = CreateTeacherRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Teacher profile created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeacherDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, user not TEACHER, user already a teacher)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TeacherDTO> createTeacher(@Valid @RequestBody CreateTeacherRequestDTO requestDTO) {
        TeacherDTO createdTeacher = teacherService.createTeacher(requestDTO);
        return new ResponseEntity<>(createdTeacher, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @teacherServiceImpl.getTeacherById(#id).getUser().getEmail() == authentication.name")
    @Operation(summary = "Get teacher profile by teacher ID", description = "Allows ADMIN, SUPER_ADMIN, or the teacher themselves to fetch the profile.")
    @Parameter(name = "id", description = "ID of the teacher profile to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved teacher profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeacherDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Teacher profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TeacherDTO> getTeacherById(@PathVariable Long id) {
        TeacherDTO teacher = teacherService.getTeacherById(id);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @userRepository.findById(#userId).orElse(null)?.email == authentication.name")
    @Operation(summary = "Get teacher profile by user ID", description = "Allows ADMIN, SUPER_ADMIN, or the teacher themselves to fetch the profile.")
    @Parameter(name = "userId", description = "User ID of the teacher to retrieve their profile", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved teacher profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeacherDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Teacher profile or User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TeacherDTO> getTeacherByUserId(@PathVariable Long userId) {
        TeacherDTO teacher = teacherService.getTeacherByUserId(userId);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all teacher profiles", description = "Requires ADMIN or SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of teacher profiles", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeacherDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        List<TeacherDTO> teachers = teacherService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @teacherServiceImpl.getTeacherById(#id).getUser().getEmail() == authentication.name")
    @Operation(
        summary = "Update a teacher's subjects or other details",
        description = "Allows ADMIN, SUPER_ADMIN, or the teacher themselves to update their profile.",
        requestBody = @RequestBody(description = "Details for updating the teacher's profile", required = true, content = @Content(schema = @Schema(implementation = UpdateTeacherRequestDTO.class)))
    )
    @Parameter(name = "id", description = "ID of the teacher profile to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Teacher profile updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeacherDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Teacher profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TeacherDTO> updateTeacher(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequestDTO requestDTO) {
        TeacherDTO updatedTeacher = teacherService.updateTeacher(id, requestDTO);
        return ResponseEntity.ok(updatedTeacher);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a teacher profile by ID", description = "Requires ADMIN or SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the teacher profile to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Teacher profile deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Teacher profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
