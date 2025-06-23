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
public class CreateFeedbackRequestDTO {
    @NotNull(message = "Student ID cannot be null")
    private Long studentId; // Student Profile ID

    @NotNull(message = "Subject Assignment ID cannot be null")
    private Long subjectAssignmentId;

    @NotBlank(message = "Feedback text cannot be blank")
    private String feedbackText;
}
