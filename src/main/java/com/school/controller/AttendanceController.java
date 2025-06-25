package com.school.controller;

import com.school.dto.AttendanceResponseDTO;
import com.school.dto.RecordAttendanceRequestDTO;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import com.school.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Corrected
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.school.dto.ErrorResponseDTO;
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
    @Operation(
        summary = "Record attendance for multiple students in a class",
        description = "Requires TEACHER role. Typically, only the designated class teacher can record attendance.",
        requestBody = @RequestBody(description = "Attendance records for a class on a specific date", required = true, content = @Content(schema = @Schema(implementation = RecordAttendanceRequestDTO.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Attendance recorded successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AttendanceResponseDTO.class))), // List
            @ApiResponse(responseCode = "400", description = "Bad Request (validation error, student not in class)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (e.g., not the class teacher)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class or Student not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<AttendanceResponseDTO>> recordAttendance(@Valid @RequestBody RecordAttendanceRequestDTO requestDTO) {
        List<AttendanceResponseDTO> savedRecords = attendanceService.recordAttendance(requestDTO, getCurrentlyLoggedInUser());
        return new ResponseEntity<>(savedRecords, HttpStatus.CREATED);
    }

    @GetMapping("/student/{studentId}/date/{date}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attendance for a specific student on a specific date", description = "Access controlled by service layer (student, parent, relevant teachers, admin).")
    @Parameter(name = "studentId", description = "ID of the student profile", required = true, in = ParameterIn.PATH)
    @Parameter(name = "date", description = "Date of attendance (YYYY-MM-DD)", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "date"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance record", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AttendanceResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student or Attendance record not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AttendanceResponseDTO> getAttendanceByStudentAndDate(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AttendanceResponseDTO attendance = attendanceService.getAttendanceByStudentAndDate(studentId, date, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/class/{classId}/date/{date}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attendance for all students in a specific class on a specific date", description = "Access controlled by service layer (class teacher, active subject teachers in class, admin).")
    @Parameter(name = "classId", description = "ID of the class", required = true, in = ParameterIn.PATH)
    @Parameter(name = "date", description = "Date of attendance (YYYY-MM-DD)", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "date"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AttendanceResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<AttendanceResponseDTO>> getAttendanceByClassAndDate(
            @PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponseDTO> attendanceList = attendanceService.getAttendanceByClassAndDate(classId, date, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(attendanceList);
    }

    @GetMapping("/student/{studentId}/period")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attendance for a specific student over a date range", description = "Access controlled by service layer.")
    @Parameter(name = "studentId", description = "ID of the student profile", required = true, in = ParameterIn.PATH)
    @Parameter(name = "startDate", description = "Start date of the period (YYYY-MM-DD)", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date"))
    @Parameter(name = "endDate", description = "End date of the period (YYYY-MM-DD)", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AttendanceResponseDTO.class))), // List
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<AttendanceResponseDTO>> getAttendanceForStudentForPeriod(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponseDTO> attendanceList = attendanceService.getAttendanceForStudentForPeriod(studentId, startDate, endDate, getCurrentlyLoggedInUser());
        return ResponseEntity.ok(attendanceList);
    }
}
