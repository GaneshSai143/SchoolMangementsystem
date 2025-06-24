package com.school.controller;

import com.school.dto.CreateTaskRequestDTO;
import com.school.dto.TaskDTO;
import com.school.dto.UpdateTaskRequestDTO;
import com.school.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.school.dto.ErrorResponseDTO;
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
    @Operation(summary = "Create a new task", description = "Requires TEACHER role. Task is assigned by the authenticated teacher. Task can be linked to a student, class, or subject assignment.")
    @SwaggerRequestBody(description = "Details of the task to be created", required = true, content = @Content(schema = @Schema(implementation = CreateTaskRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, invalid context)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (user not a teacher, or teacher not authorized for SA)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student, Class, or Subject Assignment not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        TaskDTO createdTask = taskService.createTask(requestDTO, currentUser);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get task by ID", description = "Access is handled by service layer based on user role (Admin, Teacher, or Student involved in the task).")
    @Parameter(name = "id", description = "ID of the task to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved task", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        TaskDTO task = taskService.getTaskById(id, currentUser);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get tasks, filterable by studentId, teacherId, or classId", description = "Access to filtered or all tasks is handled by service layer based on user role and involvement. Admins can see all, Teachers see tasks related to their classes/students, Students see their own.")
    @Parameter(name = "studentId", description = "Optional ID of the student to filter tasks by", in = ParameterIn.QUERY)
    @Parameter(name = "teacherId", description = "Optional ID of the teacher (profile) to filter tasks assigned by them", in = ParameterIn.QUERY)
    @Parameter(name = "classId", description = "Optional ID of the class to filter tasks by", in = ParameterIn.QUERY)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tasks", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (e.g., if trying to access all tasks without admin role)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found if filtering by an invalid ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
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
            tasks = taskService.getAllTasks(currentUser);
        }
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an existing task", description = "Access and specific field update permissions (e.g., student updating status, teacher updating full task) are handled by service layer.")
    @Parameter(name = "id", description = "ID of the task to update", required = true, in = ParameterIn.PATH)
    @SwaggerRequestBody(description = "Updated task details", required = true, content = @Content(schema = @Schema(implementation = UpdateTaskRequestDTO.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequestDTO requestDTO) {
        User currentUser = getCurrentlyLoggedInUser();
        TaskDTO updatedTask = taskService.updateTask(id, requestDTO, currentUser);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Further refined by service layer
    @Operation(summary = "Delete a task by ID", description = "Deletion permission is handled by service layer based on user role (e.g., assigning teacher or admin).")
    @Parameter(name = "id", description = "ID of the task to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        User currentUser = getCurrentlyLoggedInUser();
        taskService.deleteTask(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
