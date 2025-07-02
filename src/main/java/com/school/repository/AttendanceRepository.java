package com.school.repository;

import com.school.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);
    List<Attendance> findByClassesIdAndAttendanceDate(Long classId, LocalDate attendanceDate);
    List<Attendance> findByStudentIdAndAttendanceDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByStudent_User_IdAndAttendanceDateBetween(Long studentUserId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByRecordedByTeacherId(Long teacherId);
}
