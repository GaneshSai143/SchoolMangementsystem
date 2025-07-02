package com.school.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterMarkRequestDTO {
    @NotNull(message = "Student ID cannot be null")
    private Long studentId; // Student Profile ID

    @NotNull(message = "Subject Assignment ID cannot be null")
    private Long subjectAssignmentId;

    @NotBlank(message = "Assessment name cannot be blank")
    private String assessmentName;

    @NotNull(message = "Marks obtained cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Marks obtained must be non-negative")
    private BigDecimal marksObtained;

    @NotNull(message = "Total marks cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total marks must be positive")
    private BigDecimal totalMarks;

    private String grade;
    private LocalDate examDate;
    private String comments;
}
