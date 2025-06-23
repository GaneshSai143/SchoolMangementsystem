package com.school.service.impl;

import com.school.dto.*;
import com.school.entity.*;
import com.school.entity.enums.AttendanceStatus; // Entity Enum
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException;
import com.school.repository.*;
import com.school.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository; // For Teacher Profile
    private final SubjectAssignmentRepository subjectAssignmentRepository; // For checking active assignments
    private final ModelMapper modelMapper;
    private final UserServiceImpl userServiceImpl; // For UserDTO in nested DTOs
    // private final ClassServiceImpl classServiceImpl; // For ClassDTO - direct mapping for now

    @Override
    @Transactional
    public List<AttendanceResponseDTO> recordAttendance(RecordAttendanceRequestDTO requestDTO, User currentUser) {
        Classes aClass = classRepository.findById(requestDTO.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + requestDTO.getClassId()));

        Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("User does not have a teacher profile."));

        // Authorization: Only the designated class teacher can record attendance for this class.
        if (aClass.getClassTeacher() == null || !aClass.getClassTeacher().getId().equals(currentUser.getId())) {
            // Alternative: if any teacher teaching actively in that class can take attendance.
            // For now, strictly class teacher.
            // boolean isActiveTeacherInClass = subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(aClass.getId(), currentTeacherProfile.getId(), "ACTIVE");
            // if (!isActiveTeacherInClass) {
            //    throw new UnauthorizedActionException("Teacher is not actively assigned to this class.");
            // }
            throw new UnauthorizedActionException("User is not the designated class teacher for class: " + aClass.getName());
        }


        List<Attendance> savedRecords = new ArrayList<>();
        for (AttendanceRecordItemDTO item : requestDTO.getRecords()) {
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found with id: " + item.getStudentId()));

            if (!student.getClasses().getId().equals(aClass.getId())) {
                throw new IllegalArgumentException("Student " + student.getUser().getFirstName() + " does not belong to class " + aClass.getName());
            }

            Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(student.getId(), requestDTO.getAttendanceDate())
                    .orElse(new Attendance());

            attendance.setStudent(student);
            attendance.setClasses(aClass);
            attendance.setAttendanceDate(requestDTO.getAttendanceDate());
            attendance.setStatus(modelMapper.map(item.getStatus(), AttendanceStatus.class)); // DTO enum to Entity enum
            attendance.setRemarks(item.getRemarks());
            attendance.setRecordedByTeacher(currentTeacherProfile);

            savedRecords.add(attendanceRepository.save(attendance));
        }
        return savedRecords.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public AttendanceResponseDTO getAttendanceByStudentAndDate(Long studentId, LocalDate date, User currentUser) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + studentId));

        Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(studentId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found for student " + studentId + " on " + date));

        authorizeViewAttendance(attendance, currentUser, student);
        return convertToDTO(attendance);
    }

    @Override
    public List<AttendanceResponseDTO> getAttendanceByClassAndDate(Long classId, LocalDate date, User currentUser) {
        Classes aClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        // Authorization: Class Teacher, Principal, Super Admin
        if (currentUser.getRole() == UserRole.TEACHER) {
            if (aClass.getClassTeacher() == null || !aClass.getClassTeacher().getId().equals(currentUser.getId())) {
                 // Allow subject teachers actively teaching in this class to also view
                 Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId()).orElse(null);
                 if (currentTeacherProfile == null || !subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(classId, currentTeacherProfile.getId(), "ACTIVE")) {
                    throw new UnauthorizedActionException("Teacher is not authorized to view attendance for this class.");
                 }
            }
        } else if (currentUser.getRole() == UserRole.ADMIN) { // Principal
            if (currentUser.getSchoolId() == null || !aClass.getSchool().getId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal cannot view attendance for this class (different school).");
            }
        } else if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
             throw new UnauthorizedActionException("User not authorized to view attendance for this class.");
        }


        List<Attendance> records = attendanceRepository.findByClassesIdAndAttendanceDate(classId, date);
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AttendanceResponseDTO> getAttendanceForStudentForPeriod(Long studentId, LocalDate startDate, LocalDate endDate, User currentUser) {
         Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + studentId));

        // Authorization: Student (their own), Parent, Class Teacher, Active Subject Teacher, Principal, Super Admin
        authorizeViewAttendance(null, currentUser, student); // Pass null for attendance if it's a general check for student data access

        List<Attendance> records = attendanceRepository.findByStudentIdAndAttendanceDateBetween(studentId, startDate, endDate);
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private void authorizeViewAttendance(Attendance attendance, User currentUser, Student targetStudent) {
        // SuperAdmin/Admin (Principal of the student's school) can view all
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) return;
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() != null && targetStudent.getClasses().getSchool().getId().equals(currentUser.getSchoolId())) return;
        }

        // Student can view their own attendance
        if (currentUser.getRole() == UserRole.STUDENT && targetStudent.getUser().getId().equals(currentUser.getId())) return;

        // Teacher specific logic
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("Teacher profile not found for current user."));

            // Is it the class teacher of the student?
            User classTeacherOfStudent = targetStudent.getClasses().getClassTeacher();
            if (classTeacherOfStudent != null && classTeacherOfStudent.getId().equals(currentUser.getId())) return;

            // Is the teacher actively teaching this student (i.e., in the student's class)?
            if (subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(targetStudent.getClasses().getId(), currentTeacherProfile.getId(), "ACTIVE")) return;
        }

        // If attendance object is provided (single record view), check if current user recorded it.
        if (attendance != null && attendance.getRecordedByTeacher() != null && currentUser.getRole() == UserRole.TEACHER) {
             Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId()).orElse(null);
             if (currentTeacherProfile != null && attendance.getRecordedByTeacher().getId().equals(currentTeacherProfile.getId())) return;
        }

        throw new UnauthorizedActionException("User is not authorized to view this attendance information.");
    }

    private AttendanceResponseDTO convertToDTO(Attendance attendance) {
        AttendanceResponseDTO dto = modelMapper.map(attendance, AttendanceResponseDTO.class);

        if (attendance.getStudent() != null) {
            StudentDTO studentDTO = modelMapper.map(attendance.getStudent(), StudentDTO.class);
            if(attendance.getStudent().getUser() != null) {
                studentDTO.setUser(userServiceImpl.convertToDTO(attendance.getStudent().getUser()));
            }
             if(attendance.getStudent().getClasses() != null){ // Populate class name/id in studentDTO
                studentDTO.setClassName(attendance.getStudent().getClasses().getName());
                studentDTO.setClassId(attendance.getStudent().getClasses().getId());
            }
            dto.setStudent(studentDTO);
        }
        if (attendance.getClasses() != null) {
            dto.setClassInfo(modelMapper.map(attendance.getClasses(), ClassDTO.class));
            // If ClassDTO needs its teacher/students populated, that's more complex and handled by its own converter or service.
            // For classInfo here, a shallow map is likely sufficient.
            if (attendance.getClasses().getClassTeacher() != null) {
                dto.getClassInfo().setClassTeacher(userServiceImpl.convertToDTO(attendance.getClasses().getClassTeacher()));
            } else {
                 dto.getClassInfo().setClassTeacher(null);
            }
            dto.getClassInfo().setStudents(null); // Avoid deep nesting for this context
        }
        if (attendance.getRecordedByTeacher() != null) {
            TeacherDTO teacherDTO = modelMapper.map(attendance.getRecordedByTeacher(), TeacherDTO.class);
            if(attendance.getRecordedByTeacher().getUser() != null) {
                teacherDTO.setUser(userServiceImpl.convertToDTO(attendance.getRecordedByTeacher().getUser()));
            }
            dto.setRecordedByTeacher(teacherDTO);
        }
        if (attendance.getStatus() != null) { // Map Entity Enum to DTO Enum
            dto.setStatus(modelMapper.map(attendance.getStatus(), AttendanceStatusDTO.class));
        }
        return dto;
    }
}
