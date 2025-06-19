package com.school.controller;

import com.school.dto.CreateTeacherRequestDTO;
import com.school.dto.TeacherDTO;
import com.school.dto.UpdateTeacherRequestDTO;
import com.school.service.TeacherService;
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
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Management", description = "APIs for managing teacher profiles")
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new teacher profile", description = "Links an existing user with TEACHER role. Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<TeacherDTO> createTeacher(@Valid @RequestBody CreateTeacherRequestDTO requestDTO) {
        TeacherDTO createdTeacher = teacherService.createTeacher(requestDTO);
        return new ResponseEntity<>(createdTeacher, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @teacherServiceImpl.getTeacherById(#id).getUser().getEmail() == authentication.name")
    @Operation(summary = "Get teacher profile by teacher ID", description = "Allows ADMIN, SUPER_ADMIN, or the teacher themselves to fetch the profile.")
    public ResponseEntity<TeacherDTO> getTeacherById(@PathVariable Long id) {
        TeacherDTO teacher = teacherService.getTeacherById(id);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @userRepository.findById(#userId).orElse(null)?.email == authentication.name")
    @Operation(summary = "Get teacher profile by user ID", description = "Allows ADMIN, SUPER_ADMIN, or the teacher themselves to fetch the profile.")
    public ResponseEntity<TeacherDTO> getTeacherByUserId(@PathVariable Long userId) {
        TeacherDTO teacher = teacherService.getTeacherByUserId(userId);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all teacher profiles", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        List<TeacherDTO> teachers = teacherService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @teacherServiceImpl.getTeacherById(#id).getUser().getEmail() == authentication.name")
    @Operation(summary = "Update a teacher's subjects or other details", description = "Allows ADMIN, SUPER_ADMIN, or the teacher themselves to update their profile.")
    public ResponseEntity<TeacherDTO> updateTeacher(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequestDTO requestDTO) {
        TeacherDTO updatedTeacher = teacherService.updateTeacher(id, requestDTO);
        return ResponseEntity.ok(updatedTeacher);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a teacher profile by ID", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
