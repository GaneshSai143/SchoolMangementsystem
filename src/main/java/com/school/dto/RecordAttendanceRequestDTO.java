package com.school.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordAttendanceRequestDTO {
    @NotNull(message = "Class ID cannot be null")
    private Long classId;
    @NotNull(message = "Attendance date cannot be null")
    private LocalDate attendanceDate;
    @NotEmpty(message = "Attendance records cannot be empty")
    @Valid
    private List<AttendanceRecordItemDTO> records;
}
