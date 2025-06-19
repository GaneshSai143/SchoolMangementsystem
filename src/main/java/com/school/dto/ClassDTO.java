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
public class ClassDTO {
    private Long id;
    private String name;
    private Long schoolId; // Keep simple for now, could be SchoolDTO
    private UserDTO classTeacher; // Using UserDTO
    private List<StudentDTO> students; // Using StudentDTO
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
