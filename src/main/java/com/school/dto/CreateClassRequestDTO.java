package com.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequestDTO {
    @NotBlank(message = "Class name cannot be blank")
    private String name;
    @NotNull(message = "School ID cannot be null")
    private Long schoolId;
    private Long classTeacherId; // Optional for creation
}
