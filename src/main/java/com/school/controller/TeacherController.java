package com.school.controller;

import com.school.dto.CreateTeacherRequestDTO;
import com.school.dto.TeacherDTO;
import com.school.dto.UpdateTeacherRequestDTO;
import com.school.service.TeacherService;
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
// @Tag will be added later
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TeacherDTO> createTeacher(@Valid @RequestBody CreateTeacherRequestDTO requestDTO) {
        TeacherDTO createdTeacher = teacherService.createTeacher(requestDTO);
        return new ResponseEntity<>(createdTeacher, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @teacherServiceImpl.getTeacherById(#id).getUser().getEmail() == authentication.name")
    public ResponseEntity<TeacherDTO> getTeacherById(@PathVariable Long id) {
        TeacherDTO teacher = teacherService.getTeacherById(id);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @userRepository.findById(#userId).orElse(null)?.email == authentication.name")
    public ResponseEntity<TeacherDTO> getTeacherByUserId(@PathVariable Long userId) {
        TeacherDTO teacher = teacherService.getTeacherByUserId(userId);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        List<TeacherDTO> teachers = teacherService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @teacherServiceImpl.getTeacherById(#id).getUser().getEmail() == authentication.name")
    public ResponseEntity<TeacherDTO> updateTeacher(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequestDTO requestDTO) {
        TeacherDTO updatedTeacher = teacherService.updateTeacher(id, requestDTO);
        return ResponseEntity.ok(updatedTeacher);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
