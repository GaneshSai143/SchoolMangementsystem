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
public class SchoolDTO {
    private Long id;
    private String name;
    private String location;
    private UserDTO principal; // Using UserDTO for principal
    private List<ClassDTO> classes; // Using ClassDTO for classes
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
