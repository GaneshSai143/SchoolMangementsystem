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
public class SubjectAssignmentResponseDTO {
    private Long id;
    private ClassDTO classDTO; // Using the existing ClassDTO
    private SubjectResponseDTO subjectDTO; // Using the existing SubjectResponseDTO
    private TeacherDTO teacherDTO; // Using the existing TeacherDTO
    private String academicYear;
    private String term;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
