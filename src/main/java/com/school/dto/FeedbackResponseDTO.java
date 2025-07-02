package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponseDTO {
    private Long id;
    private StudentDTO student; // Student Profile DTO
    private SubjectAssignmentResponseDTO subjectAssignment; // Includes Class, Subject, Teacher DTOs
    private String feedbackText;
    private LocalDateTime submissionDate;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
