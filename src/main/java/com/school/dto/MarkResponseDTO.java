package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkResponseDTO {
    private Long id;
    private StudentDTO student;
    private SubjectAssignmentResponseDTO subjectAssignment; // Contains Class, Subject, Teacher info
    private String assessmentName;
    private BigDecimal marksObtained;
    private BigDecimal totalMarks;
    private String grade;
    private LocalDate examDate;
    private String comments;
    private TeacherDTO recordedByTeacher; // Info of teacher who recorded
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
