package com.school.controller;

import com.school.dto.SubjectRequestDTO;
import com.school.dto.SubjectResponseDTO;
import com.school.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Or a dedicated 'CURRICULUM_MANAGER' role
    @Operation(summary = "Create a new subject", description = "Accessible by SUPER_ADMIN.")
    public ResponseEntity<SubjectResponseDTO> createSubject(@Valid @RequestBody SubjectRequestDTO requestDTO) {
        SubjectResponseDTO createdSubject = subjectService.createSubject(requestDTO);
        return new ResponseEntity<>(createdSubject, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // All authenticated users can view subjects
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<SubjectResponseDTO> getSubjectById(@PathVariable Long id) {
        SubjectResponseDTO subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(subject);
    }

    @GetMapping("/code/{subjectCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subject by subject code")
    public ResponseEntity<SubjectResponseDTO> getSubjectByCode(@PathVariable String subjectCode) {
        SubjectResponseDTO subject = subjectService.getSubjectByCode(subjectCode);
        return ResponseEntity.ok(subject);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all subjects")
    public ResponseEntity<List<SubjectResponseDTO>> getAllSubjects() {
        List<SubjectResponseDTO> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update an existing subject", description = "Accessible by SUPER_ADMIN.")
    public ResponseEntity<SubjectResponseDTO> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequestDTO requestDTO) {
        SubjectResponseDTO updatedSubject = subjectService.updateSubject(id, requestDTO);
        return ResponseEntity.ok(updatedSubject);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a subject by ID", description = "Accessible by SUPER_ADMIN. Deletion might be restricted if subject is in use.")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
