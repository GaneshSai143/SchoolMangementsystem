package com.school.service;

import com.school.dto.AttendanceResponseDTO;
import com.school.dto.RecordAttendanceRequestDTO;
import com.school.entity.User;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    List<AttendanceResponseDTO> recordAttendance(RecordAttendanceRequestDTO requestDTO, User currentUser);
    AttendanceResponseDTO getAttendanceByStudentAndDate(Long studentId, LocalDate date, User currentUser);
    List<AttendanceResponseDTO> getAttendanceByClassAndDate(Long classId, LocalDate date, User currentUser);
    List<AttendanceResponseDTO> getAttendanceForStudentForPeriod(Long studentId, LocalDate startDate, LocalDate endDate, User currentUser);
}
