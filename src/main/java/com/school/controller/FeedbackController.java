package com.school.controller;

import com.school.dto.CreateFeedbackRequestDTO;
import com.school.dto.FeedbackResponseDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.FeedbackService;
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
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback Management", description = "APIs for managing feedback from teachers to students")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + userEmail));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Submit feedback for a student regarding a subject assignment", description = "Only the teacher assigned to the SubjectAssignment can submit feedback. Student must belong to the class of the assignment.")
    @RequestBody(description = "Details of the feedback to be submitted", required = true, content = @Content(schema = @Schema(implementation = CreateFeedbackRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Feedback submitted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedbackResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, student not in class)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (e.g., not the assigned teacher)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student, SubjectAssignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<FeedbackResponseDTO> submitFeedback(@Valid @RequestBody CreateFeedbackRequestDTO requestDTO) {
        FeedbackResponseDTO feedback = feedbackService.submitFeedback(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(feedback, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get specific feedback by its ID", description = "Access controlled by service layer (student, relevant teachers, admin).")
    @Parameter(name = "id", description = "ID of the feedback to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved feedback", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedbackResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Feedback not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<FeedbackResponseDTO> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all feedback for a specific student (profile ID)", description = "Access controlled by service layer (student, relevant teachers, admin).")
    @Parameter(name = "studentId", description = "ID of the student profile", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved feedback list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedbackResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByStudentId(studentId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/class-teacher/me")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get all feedback for students where the current user is the class teacher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved feedback list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedbackResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user not a teacher)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackForMyStudentsAsClassTeacher() {
        return ResponseEntity.ok(feedbackService.getFeedbackForClassTeacher(getCurrentlyLoggedInUser()));
    }

    @GetMapping("/class-teacher/me/unread")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get unread feedback for students where the current user is the class teacher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved unread feedback list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedbackResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user not a teacher)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<FeedbackResponseDTO>> getUnreadFeedbackForMyStudentsAsClassTeacher() {
        return ResponseEntity.ok(feedbackService.getUnreadFeedbackForClassTeacher(getCurrentlyLoggedInUser()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Mark feedback as read (for class teachers)", description = "Only the class teacher of the student to whom the feedback pertains can mark it as read.")
    @Parameter(name = "id", description = "ID of the feedback to mark as read", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback marked as read successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedbackResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user not the relevant class teacher)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Feedback not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<FeedbackResponseDTO> markFeedbackAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.markFeedbackAsRead(id, getCurrentlyLoggedInUser()));
    }
}
