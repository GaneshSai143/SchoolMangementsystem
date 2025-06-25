package com.school.controller;

import com.school.dto.CreateParentByAdminRequestDTO; // Specific
import com.school.dto.CreatePrincipalRequestDTO; // Specific
import com.school.dto.CreateStudentByAdminRequestDTO; // Specific
import com.school.dto.CreateTeacherByAdminRequestDTO; // Specific
import com.school.dto.ErrorResponseDTO; // Specific
import com.school.dto.StudentDTO; // Specific
import com.school.dto.TeacherDTO; // Specific
import com.school.dto.UserDTO; // Specific
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Correct import
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "APIs for administrators to create Principals, Teachers, and Students")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;


    // Helper method to get the full User entity for the logged-in admin
    private User getCurrentlyLoggedInUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found with email: " + userEmail));
    }


    @PostMapping("/principals")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Create a new Principal (School Admin)",
        description = "Accessible only by SUPER_ADMIN. Links the Principal to a specified school.",
        requestBody = @RequestBody(description = "Details of the Principal to be created", required = true, content = @Content(schema = @Schema(implementation = CreatePrincipalRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Principal created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, email exists, school not found)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (authentication required or failed)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user lacks SUPER_ADMIN role)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not Found (e.g., specified school ID does not exist)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDTO> createPrincipal(@Valid @RequestBody CreatePrincipalRequestDTO requestDTO) {
        UserDTO newPrincipal = adminService.createPrincipal(requestDTO);
        return new ResponseEntity<>(newPrincipal, HttpStatus.CREATED);
    }

    @PostMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')") // School Admin (Principal)
    @Operation(
        summary = "Create a new Teacher",
        description = "Accessible only by ADMIN (Principal). Teacher is associated with the Principal's school.",
        requestBody = @RequestBody(description = "Details of the Teacher to be created", required = true, content = @Content(schema = @Schema(implementation = CreateTeacherByAdminRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Teacher created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeacherDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, email exists)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user lacks ADMIN role or admin not linked to school)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TeacherDTO> createTeacherByAdmin(@Valid @RequestBody CreateTeacherByAdminRequestDTO requestDTO) {
        User loggedInAdmin = getCurrentlyLoggedInUserEntity();
        TeacherDTO newTeacher = adminService.createTeacherByAdmin(requestDTO, loggedInAdmin);
        return new ResponseEntity<>(newTeacher, HttpStatus.CREATED);
    }

    @PostMapping("/students")
    @PreAuthorize("hasRole('ADMIN')") // School Admin (Principal)
    @Operation(
        summary = "Create a new Student",
        description = "Accessible only by ADMIN (Principal). Student is associated with the Principal's school and assigned to a class.",
        requestBody = @RequestBody(description = "Details of the Student to be created", required = true, content = @Content(schema = @Schema(implementation = CreateStudentByAdminRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, email exists, class not in admin's school)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not Found (e.g., specified class ID does not exist)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<StudentDTO> createStudentByAdmin(@Valid @RequestBody CreateStudentByAdminRequestDTO requestDTO) {
        User loggedInAdmin = getCurrentlyLoggedInUserEntity();
        StudentDTO newStudent = adminService.createStudentByAdmin(requestDTO, loggedInAdmin);
        return new ResponseEntity<>(newStudent, HttpStatus.CREATED);
    }

    @PostMapping("/parents")
    @PreAuthorize("hasRole('ADMIN')") // School Admin (Principal)
    @Operation(
        summary = "Create a new Parent user",
        description = "Accessible only by ADMIN (Principal). Parent is associated with the Principal's school.",
        requestBody = @RequestBody(description = "Details of the Parent to be created", required = true, content = @Content(schema = @Schema(implementation = CreateParentByAdminRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Parent created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, email exists)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDTO> createParentByAdmin(@Valid @RequestBody CreateParentByAdminRequestDTO requestDTO) {
        User loggedInAdmin = getCurrentlyLoggedInUserEntity(); // Existing helper method
        UserDTO newParent = adminService.createParentByAdmin(requestDTO, loggedInAdmin);
        return new ResponseEntity<>(newParent, HttpStatus.CREATED);
    }
}
