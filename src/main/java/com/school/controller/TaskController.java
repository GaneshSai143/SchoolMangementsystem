package com.school.controller;

import com.school.dto.CreateTaskRequestDTO;
import com.school.dto.TaskDTO;
import com.school.dto.UpdateTaskRequestDTO;
import com.school.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
// @Tag will be added later
public class TaskController {

    private final TaskService taskService;

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequestDTO requestDTO) {
        TaskDTO createdTask = taskService.createTask(requestDTO, getAuthenticatedUsername());
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDTO>> getTasks(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId) {
        List<TaskDTO> tasks;
        if (studentId != null) {
            tasks = taskService.getTasksByStudentId(studentId);
        } else if (teacherId != null) {
            tasks = taskService.getTasksByTeacherId(teacherId);
        } else if (classId != null) {
            tasks = taskService.getTasksByClassId(classId);
        } else {
            // This might be too broad for non-admins. Add role checks in service or here.
            // For now, only admins can see all tasks without filter.
            // Others should use specific filters.
            // Service layer should enforce that non-admins cannot call getAllTasks()
            tasks = taskService.getAllTasks();
        }
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequestDTO requestDTO) {
        TaskDTO updatedTask = taskService.updateTask(id, requestDTO, getAuthenticatedUsername());
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id, getAuthenticatedUsername());
        return ResponseEntity.noContent().build();
    }
}
