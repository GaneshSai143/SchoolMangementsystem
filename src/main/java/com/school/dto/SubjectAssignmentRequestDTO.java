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
public class SubjectAssignmentRequestDTO {
    @NotNull(message = "Class ID cannot be null")
    private Long classId;

    @NotNull(message = "Subject ID cannot be null")
    private Long subjectId;

    @NotNull(message = "Teacher ID cannot be null")
    private Long teacherId; // This is the ID of the Teacher profile

    @NotBlank(message = "Academic year cannot be blank")
    private String academicYear;

    @NotBlank(message = "Term cannot be blank")
    private String term;

    private String status; // Optional on creation, defaults to ACTIVE
}
