package com.school.controller;

import com.school.dto.CreateTaskRequestDTO;
import com.school.dto.TaskDTO;
import com.school.dto.UpdateTaskRequestDTO;
import com.school.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.school.entity.User; // Added
import com.school.repository.UserRepository; // Added
import com.school.exception.ResourceNotFoundException; // Added
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "APIs for managing tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository; // Added

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + userEmail));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a new task", description = "Requires TEACHER role. Task is assigned by the authenticated teacher.")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        TaskDTO createdTask = taskService.createTask(requestDTO, currentUser);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get task by ID", description = "Access is handled by service layer based on user role (Admin, Teacher, or Student involved in the task).")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        TaskDTO task = taskService.getTaskById(id, currentUser);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get tasks, filterable by studentId, teacherId, or classId", description = "Access to filtered or all tasks is handled by service layer based on user role and involvement.")
    public ResponseEntity<List<TaskDTO>> getTasks(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId) {
        User currentUser = getCurrentlyLoggedInUser();
        List<TaskDTO> tasks;
        if (studentId != null) {
            tasks = taskService.getTasksByStudentId(studentId, currentUser);
        } else if (teacherId != null) {
            tasks = taskService.getTasksByTeacherId(teacherId, currentUser);
        } else if (classId != null) {
            tasks = taskService.getTasksByClassId(classId, currentUser);
        } else {
            // Service layer should enforce that non-admins cannot call getAllTasks() without appropriate rights.
            tasks = taskService.getAllTasks(currentUser);
        }
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an existing task", description = "Access and specific field update permissions (e.g., student updating status) are handled by service layer.")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        TaskDTO updatedTask = taskService.updateTask(id, requestDTO, currentUser);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a task by ID", description = "Deletion permission is handled by service layer based on user role (e.g., assigning teacher or admin).")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        taskService.deleteTask(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
