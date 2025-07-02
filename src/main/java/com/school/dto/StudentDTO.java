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
public class StudentDTO {
    private Long id;
    private UserDTO user; // User details of the student
    private Long classId; // ID of the class
    private String className; // Name of the class
    // private List<TaskDTO> tasks; // Avoid deep nesting for now, could be separate endpoint
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
