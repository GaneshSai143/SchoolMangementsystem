package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDTO {
    private Long id;
    private UserDTO user; // User details of the teacher
    private List<String> subjects;
    // private List<TaskDTO> assignedTasks; // Avoid deep nesting
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
