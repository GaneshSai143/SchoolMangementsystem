package com.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeacherRequestDTO {
    @NotNull(message = "User ID cannot be null")
    private Long userId; // Refers to an existing User entity
    private List<String> subjects;
}
