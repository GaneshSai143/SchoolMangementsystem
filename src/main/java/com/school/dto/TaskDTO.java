package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.school.dto.SubjectAssignmentResponseDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private TaskStatusDTO status;
    private TaskPriorityDTO priority;
    private Long studentId;
    private String studentName; // Denormalized for convenience
    private Long teacherId;
    private String teacherName; // Denormalized for convenience
    private Long classId;
    private String className; // Denormalized for convenience
    private SubjectAssignmentResponseDTO subjectAssignment; // Newly added
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
