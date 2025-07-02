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
public class CreateSchoolRequestDTO {
    @NotBlank(message = "School name cannot be blank")
    private String name;
    @NotBlank(message = "School location cannot be blank")
    private String location;
    @NotNull(message = "Principal ID cannot be null")
    private Long principalId;
}
