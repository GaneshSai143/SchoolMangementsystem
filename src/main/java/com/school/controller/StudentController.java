package com.school.controller;

import com.school.dto.CreateStudentRequestDTO;
import com.school.dto.StudentDTO;
import com.school.dto.UpdateStudentRequestDTO;
import com.school.service.StudentService;
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

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "APIs for managing student profiles")
public class StudentController {

    private final StudentService studentService;
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
        summary = "Create a new student profile",
        description = "Links an existing user with STUDENT role to a class. Requires ADMIN or SUPER_ADMIN role.",
        requestBody = @RequestBody(description = "Details of the student to be created", required = true, content = @Content(schema = @Schema(implementation = CreateStudentRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student profile created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, user not STUDENT, user already a student, class not found)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or Class not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<StudentDTO> createStudent(@Valid @org.springframework.web.bind.annotation.RequestBody CreateStudentRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        StudentDTO createdStudent = studentService.createStudent(requestDTO, currentUser);
        return new ResponseEntity<>(createdStudent, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER') or @studentServiceImpl.getStudentById(#id).getUser().getEmail() == authentication.name")
    @Operation(summary = "Get student profile by student ID", description = "Allows ADMIN, SUPER_ADMIN, relevant TEACHER, or the student themselves to fetch the profile. Access for teachers and students is further validated by service layer.")
    @Parameter(name = "id", description = "ID of the student profile to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        StudentDTO student = studentService.getStudentById(id, currentUser);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER') or @userRepository.findById(#userId).orElse(null)?.email == authentication.name")
    @Operation(summary = "Get student profile by user ID", description = "Allows ADMIN, SUPER_ADMIN, relevant TEACHER, or the student themselves to fetch the profile. Access for teachers and students is further validated by service layer.")
    @Parameter(name = "userId", description = "User ID of the student to retrieve their profile", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student profile or User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<StudentDTO> getStudentByUserId(@PathVariable Long userId) {
        User currentUser = getCurrentlyLoggedInUser();
        StudentDTO student = studentService.getStudentByUserId(userId, currentUser);
        return ResponseEntity.ok(student);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    @Operation(summary = "Get all student profiles, optionally filtered by class ID", description = "Teacher access is restricted by service logic to students in classes they actively teach.")
    @Parameter(name = "classId", description = "Optional ID of the class to filter students by", required = false, in = ParameterIn.QUERY)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student profiles", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class))), // List of StudentDTO
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class not found if classId is provided and invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<StudentDTO>> getAllStudents(@RequestParam(required = false) Long classId) {
        User currentUser = getCurrentlyLoggedInUser();
        List<StudentDTO> students;
        if (classId != null) {
            students = studentService.getStudentsByClassId(classId, currentUser);
        } else {
            students = studentService.getAllStudents(currentUser);
        }
        return ResponseEntity.ok(students);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Update a student's class assignment",
        description = "Requires ADMIN or SUPER_ADMIN role.",
        requestBody = @RequestBody(description = "Details for updating the student's class", required = true, content = @Content(schema = @Schema(implementation = UpdateStudentRequestDTO.class)))
    )
    @Parameter(name = "id", description = "ID of the student profile to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student profile updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student profile or Class not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @org.springframework.web.bind.annotation.RequestBody UpdateStudentRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        StudentDTO updatedStudent = studentService.updateStudent(id, requestDTO, currentUser);
        return ResponseEntity.ok(updatedStudent);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a student profile by ID", description = "Requires ADMIN or SUPER_ADMIN role.")
    @Parameter(name = "id", description = "ID of the student profile to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Student profile deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student profile not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        studentService.deleteStudent(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
