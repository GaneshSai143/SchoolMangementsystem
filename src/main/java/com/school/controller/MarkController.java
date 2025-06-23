package com.school.controller;

import com.school.dto.EnterMarkRequestDTO;
import com.school.dto.MarkResponseDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.MarkService;
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
    @Operation(summary = "Enter marks for a student for a specific subject assignment.")
    public ResponseEntity<MarkResponseDTO> enterMarks(@Valid @RequestBody EnterMarkRequestDTO requestDTO) {
        MarkResponseDTO savedMark = markService.enterMarks(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(savedMark, HttpStatus.CREATED);
    }

    @PutMapping("/{markId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')") // Service layer has finer checks
    @Operation(summary = "Update an existing mark record.")
    public ResponseEntity<MarkResponseDTO> updateMarks(@PathVariable Long markId, @Valid @RequestBody EnterMarkRequestDTO requestDTO) {
        MarkResponseDTO updatedMark = markService.updateMarks(markId, requestDTO, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(updatedMark);
    }

    @GetMapping("/{markId}")
    @PreAuthorize("isAuthenticated()") // Service layer handles specific auth
    @Operation(summary = "Get a specific mark record by its ID.")
    public ResponseEntity<MarkResponseDTO> getMarkById(@PathVariable Long markId) {
        return ResponseEntity.ok(markService.getMarkById(markId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/student/profile/{studentProfileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all marks for a student (by student profile ID).")
    public ResponseEntity<List<MarkResponseDTO>> getMarksByStudentProfileId(@PathVariable Long studentProfileId) {
        return ResponseEntity.ok(markService.getMarksByStudentId(studentProfileId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/student/user/{studentUserId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all marks for a student (by main user ID).")
    public ResponseEntity<List<MarkResponseDTO>> getMarksByStudentUserId(@PathVariable Long studentUserId) {
        return ResponseEntity.ok(markService.getMarksByStudentUserId(studentUserId, getCurrentlyLoggedInUser()));
    }

    @GetMapping("/subject-assignment/{saId}")
    @PreAuthorize("isAuthenticated()") // Service layer handles specific auth
    @Operation(summary = "Get all marks for a specific subject assignment.")
    public ResponseEntity<List<MarkResponseDTO>> getMarksBySubjectAssignmentId(@PathVariable Long saId) {
        return ResponseEntity.ok(markService.getMarksBySubjectAssignmentId(saId, getCurrentlyLoggedInUser()));
    }

    @DeleteMapping("/{markId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')") // Service layer has finer checks
    @Operation(summary = "Delete a mark record by its ID.")
    public ResponseEntity<Void> deleteMark(@PathVariable Long markId){
        markService.deleteMark(markId, getCurrentlyLoggedInUser());
        return ResponseEntity.noContent().build();
    }
}
