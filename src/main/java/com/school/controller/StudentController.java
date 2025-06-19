package com.school.controller;

import com.school.dto.CreateStudentRequestDTO;
import com.school.dto.StudentDTO;
import com.school.dto.UpdateStudentRequestDTO;
import com.school.service.StudentService;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "APIs for managing student profiles")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new student profile", description = "Links an existing user with STUDENT role to a class. Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody CreateStudentRequestDTO requestDTO) {
        StudentDTO createdStudent = studentService.createStudent(requestDTO);
        return new ResponseEntity<>(createdStudent, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER') or @studentServiceImpl.getStudentById(#id).getUser().getEmail() == authentication.name")
    @Operation(summary = "Get student profile by student ID", description = "Allows ADMIN, SUPER_ADMIN, relevant TEACHER, or the student themselves to fetch the profile.")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        StudentDTO student = studentService.getStudentById(id);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER') or @userRepository.findById(#userId).orElse(null)?.email == authentication.name")
    @Operation(summary = "Get student profile by user ID", description = "Allows ADMIN, SUPER_ADMIN, relevant TEACHER, or the student themselves to fetch the profile.")
    public ResponseEntity<StudentDTO> getStudentByUserId(@PathVariable Long userId) {
        StudentDTO student = studentService.getStudentByUserId(userId);
        return ResponseEntity.ok(student);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    @Operation(summary = "Get all student profiles, optionally filtered by class ID", description = "Teacher access may be restricted by service logic to their specific classes.")
    public ResponseEntity<List<StudentDTO>> getAllStudents(@RequestParam(required = false) Long classId) {
        List<StudentDTO> students;
        if (classId != null) {
            students = studentService.getStudentsByClassId(classId);
        } else {
            students = studentService.getAllStudents();
        }
        return ResponseEntity.ok(students);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update a student's class assignment", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequestDTO requestDTO) {
        StudentDTO updatedStudent = studentService.updateStudent(id, requestDTO);
        return ResponseEntity.ok(updatedStudent);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a student profile by ID", description = "Requires ADMIN or SUPER_ADMIN role.")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
