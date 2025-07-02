package com.school.controller;

import com.school.dto.ClassDTO;
import com.school.dto.CreateClassRequestDTO;
import com.school.dto.UpdateClassRequestDTO;
import com.school.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Corrected
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

import com.school.entity.User;
import com.school.repository.UserRepository;
import com.school.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "Class Management", description = "APIs for managing classes within schools")
public class ClassController {

    private final ClassService classService;
    private final UserRepository userRepository;

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + userEmail));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Create a new class",
        description = "Requires ADMIN or SUPER_ADMIN role. ADMIN (Principal) can only create for their own school.",
        requestBody = @RequestBody(description = "Details of the class to be created", required = true, content = @Content(schema = @Schema(implementation = CreateClassRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Class created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "School or Teacher not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ClassDTO> createClass(@Valid @RequestBody CreateClassRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        ClassDTO createdClass = classService.createClass(requestDTO, currentUser);
        return new ResponseEntity<>(createdClass, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    @Operation(summary = "Get class by ID", description = "Requires ADMIN, SUPER_ADMIN, or TEACHER role. Teacher access is restricted to classes they are actively assigned to.")
    @Parameter(name = "id", description = "ID of the class to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved class", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ClassDTO> getClassById(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        ClassDTO classDTO = classService.getClassById(id, currentUser);
        return ResponseEntity.ok(classDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    @Operation(summary = "Get all classes, optionally filtered by school ID", description = "Requires ADMIN, SUPER_ADMIN, or TEACHER role. Teacher access is restricted to classes they are actively assigned to within the specified school (if any) or across all schools they teach in.")
    @Parameter(name = "schoolId", description = "Optional ID of the school to filter classes by", required = false, in = ParameterIn.QUERY)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved classes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassDTO.class))), // List of ClassDTO
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "School not found if schoolId is provided and invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<ClassDTO>> getAllClasses(@RequestParam(required = false) Long schoolId) {
        User currentUser = getCurrentlyLoggedInUser();
        List<ClassDTO> classes;
        if (schoolId != null) {
            classes = classService.getClassesBySchoolId(schoolId, currentUser);
        } else {
            classes = classService.getAllClasses(currentUser);
        }
        return ResponseEntity.ok(classes);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Update an existing class",
        description = "Requires ADMIN or SUPER_ADMIN role. ADMIN (Principal) can only update classes in their own school (validated in service).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated class details", required = true, content = @Content(schema = @Schema(implementation = UpdateClassRequestDTO.class)))
    )
    @Parameter(name = "id", description = "ID of the class to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class or Teacher not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ClassDTO> updateClass(@PathVariable Long id, @Valid @org.springframework.web.bind.annotation.RequestBody UpdateClassRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        ClassDTO updatedClass = classService.updateClass(id, requestDTO, currentUser);
        return ResponseEntity.ok(updatedClass);
    }

    @PatchMapping("/{classId}/assign-teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Assign a teacher to a class",
        description = "Requires ADMIN or SUPER_ADMIN role. ADMIN (Principal) can only assign for classes/teachers in their own school (validated in service).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payload containing teacherId. Example: {\"teacherId\": 123}", required = true, content = @Content(mediaType = "application/json", schema = @Schema(type="object", example = "{\"teacherId\": 123}")))
    )
    @Parameter(name = "classId", description = "ID of the class to assign a teacher to", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Teacher assigned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g. missing teacherId)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class or Teacher not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ClassDTO> assignClassTeacher(@PathVariable Long classId, @org.springframework.web.bind.annotation.RequestBody Map<String, Long> payload) {
        Long teacherId = payload.get("teacherId");
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId must be provided in the payload.");
        }
        User currentUser = getCurrentlyLoggedInUser();
        ClassDTO updatedClass = classService.assignClassTeacher(classId, teacherId, currentUser);
        return ResponseEntity.ok(updatedClass);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a class by ID", description = "Requires ADMIN or SUPER_ADMIN role. ADMIN (Principal) can only delete classes in their own school (validated in service).")
    @Parameter(name = "id", description = "ID of the class to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Class deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        classService.deleteClass(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
