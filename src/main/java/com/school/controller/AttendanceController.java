package com.school.controller;

import com.school.dto.AttendanceResponseDTO;
import com.school.dto.RecordAttendanceRequestDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance Management", description = "APIs for recording and viewing student attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    private User getCurrentlyLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + userEmail));
    }

    @PostMapping("/record")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Record attendance for multiple students in a class for a specific date.")
    public ResponseEntity<List<AttendanceResponseDTO>> recordAttendance(@Valid @RequestBody RecordAttendanceRequestDTO requestDTO) {
        List<AttendanceResponseDTO> savedRecords = attendanceService.recordAttendance(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(savedRecords, HttpStatus.CREATED);
    }

    @GetMapping("/student/{studentId}/date/{date}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attendance for a specific student on a specific date.")
    public ResponseEntity<AttendanceResponseDTO> getAttendanceByStudentAndDate(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AttendanceResponseDTO attendance = attendanceService.getAttendanceByStudentAndDate(studentId, date, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/class/{classId}/date/{date}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attendance for all students in a specific class on a specific date.")
    public ResponseEntity<List<AttendanceResponseDTO>> getAttendanceByClassAndDate(
            @PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponseDTO> attendanceList = attendanceService.getAttendanceByClassAndDate(classId, date, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(attendanceList);
    }

    @GetMapping("/student/{studentId}/period")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attendance for a specific student over a date range.")
    public ResponseEntity<List<AttendanceResponseDTO>> getAttendanceForStudentForPeriod(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponseDTO> attendanceList = attendanceService.getAttendanceForStudentForPeriod(studentId, startDate, endDate, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(attendanceList);
    }
}
