package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDTO {
    private Long id;
    private StudentDTO student; // Full StudentDTO
    private ClassDTO classInfo; // Full ClassDTO
    private LocalDate attendanceDate;
    private AttendanceStatusDTO status;
    private String remarks;
    private TeacherDTO recordedByTeacher; // Full TeacherDTO
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
