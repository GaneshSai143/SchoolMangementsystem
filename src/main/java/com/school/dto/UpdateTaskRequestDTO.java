package com.school.dto;

import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequestDTO {
    private String title;
    private String description;
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;
    private TaskStatusDTO status;
    private TaskPriorityDTO priority;
    // studentId, classId, teacherId generally shouldn't be updated directly on a task
}
