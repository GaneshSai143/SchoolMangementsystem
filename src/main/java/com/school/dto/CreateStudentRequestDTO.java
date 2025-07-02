package com.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequestDTO {
    @NotNull(message = "User ID cannot be null")
    private Long userId; // Refers to an existing User entity
    @NotNull(message = "Class ID cannot be null")
    private Long classId;
}
