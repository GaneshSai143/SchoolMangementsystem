package com.school.controller;

import com.school.dto.ClassDTO;
import com.school.dto.CreateClassRequestDTO;
import com.school.dto.UpdateClassRequestDTO;
import com.school.service.ClassService;
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

import com.school.entity.User;
import com.school.repository.UserRepository;
import com.school.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
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
    @Operation(summary = "Create a new class", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<ClassDTO> createClass(@Valid @RequestBody CreateClassRequestDTO requestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + userEmail));

        ClassDTO createdClass = classService.createClass(requestDTO, currentUser);
        return new ResponseEntity<>(createdClass, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    @Operation(summary = "Get class by ID", description = "Requires ADMIN, SUPER_ADMIN, or TEACHER role. Teacher access may be further restricted by service logic.")
    public ResponseEntity<ClassDTO> getClassById(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        ClassDTO classDTO = classService.getClassById(id, currentUser);
        return ResponseEntity.ok(classDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    @Operation(summary = "Get all classes, optionally filtered by school ID", description = "Requires ADMIN, SUPER_ADMIN, or TEACHER role. Teacher access may be further restricted.")
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
    @Operation(summary = "Update an existing class", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<ClassDTO> updateClass(@PathVariable Long id, @Valid @RequestBody UpdateClassRequestDTO requestDTO) {
        ClassDTO updatedClass = classService.updateClass(id, requestDTO);
        return ResponseEntity.ok(updatedClass);
    }

    @PatchMapping("/{classId}/assign-teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Assign a teacher to a class", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<ClassDTO> assignClassTeacher(@PathVariable Long classId, @RequestBody Map<String, Long> payload) {
        Long teacherId = payload.get("teacherId");
        if (teacherId == null) {
            return ResponseEntity.badRequest().build(); // Or throw a specific exception
        }
        ClassDTO updatedClass = classService.assignClassTeacher(classId, teacherId);
        return ResponseEntity.ok(updatedClass);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a class by ID", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }
}
