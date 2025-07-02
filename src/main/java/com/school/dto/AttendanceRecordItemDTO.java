package com.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordItemDTO {
    @NotNull(message = "Student ID cannot be null")
    private Long studentId; // Student Profile ID
    @NotNull(message = "Status cannot be null")
    private AttendanceStatusDTO status;
    private String remarks;
}
