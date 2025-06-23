package com.school.controller;

import com.school.dto.CreateFeedbackRequestDTO;
import com.school.dto.FeedbackResponseDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.FeedbackService;
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
    @Operation(summary = "Submit feedback for a student regarding a subject assignment")
    public ResponseEntity<FeedbackResponseDTO> submitFeedback(@Valid @RequestBody CreateFeedbackRequestDTO requestDTO) {
        FeedbackResponseDTO feedback = feedbackService.submitFeedback(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(feedback, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Service layer handles specific auth
    @Operation(summary = "Get specific feedback by its ID")
    public ResponseEntity<FeedbackResponseDTO> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()") // Service layer handles specific auth
    @Operation(summary = "Get all feedback for a specific student (profile ID)")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByStudentId(studentId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/class-teacher/me")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get all feedback for students where the current user is the class teacher")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackForMyStudentsAsClassTeacher() {
        return ResponseEntity.ok(feedbackService.getFeedbackForClassTeacher(getCurrentlyLoggedInUser()));
    }

    @GetMapping("/class-teacher/me/unread")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get unread feedback for students where the current user is the class teacher")
    public ResponseEntity<List<FeedbackResponseDTO>> getUnreadFeedbackForMyStudentsAsClassTeacher() {
        return ResponseEntity.ok(feedbackService.getUnreadFeedbackForClassTeacher(getCurrentlyLoggedInUser()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Mark feedback as read (for class teachers)")
    public ResponseEntity<FeedbackResponseDTO> markFeedbackAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.markFeedbackAsRead(id, getCurrentlyLoggedInUser()));
    }
}
