package com.school.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequestDTO {
    @NotBlank(message = "Title cannot be blank")
    private String title;
    private String description;
    @NotNull(message = "Due date cannot be null")
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;
    @NotNull(message = "Status cannot be null")
    private TaskStatusDTO status;
    private TaskPriorityDTO priority;
    private Long studentId; // Optional: task can be for a class or specific student
    private Long classId;   // Optional: task can be for a class
    private Long subjectAssignmentId; // Newly added optional field
    // TeacherId will be inferred from authenticated user (if teacher creates it)
}
