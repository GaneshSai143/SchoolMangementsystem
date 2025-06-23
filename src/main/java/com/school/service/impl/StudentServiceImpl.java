package com.school.service.impl;

import com.school.dto.CreateStudentRequestDTO;
import com.school.dto.StudentDTO;
import com.school.dto.UpdateStudentRequestDTO;
import com.school.dto.UserDTO; // Required for mapping
import com.school.entity.Classes;
import com.school.entity.Student;
import com.school.entity.User;
import com.school.entity.UserRole;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException; // Added
import com.school.repository.*; // Added Teacher and SubjectAssignment Repos
import com.school.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository; // Added
    private final SubjectAssignmentRepository subjectAssignmentRepository; // Added
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public StudentDTO createStudent(CreateStudentRequestDTO requestDTO) {
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestDTO.getUserId()));

        // Ensure user has a STUDENT role or set it
        if (user.getRole() != UserRole.STUDENT) {
            // This could be an error, or an automatic role assignment
            // For now, let's assume an error if user is not already a student type
             throw new IllegalArgumentException("User with ID " + requestDTO.getUserId() + " does not have the STUDENT role.");
        }

        if (studentRepository.existsByUserId(requestDTO.getUserId())) {
            throw new IllegalArgumentException("User with ID " + requestDTO.getUserId() + " is already registered as a student.");
        }

        Classes studentClass = classRepository.findById(requestDTO.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + requestDTO.getClassId()));

        Student student = Student.builder()
                .user(user)
                .classes(studentClass)
                .build();

        Student savedStudent = studentRepository.save(student);
        return convertToDTO(savedStudent);
    }

    @Override
    public StudentDTO getStudentById(Long id, User currentUser) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (student.getClasses() == null || !subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(student.getClasses().getId(), teacherProfile.getId(), "ACTIVE")) {
                throw new UnauthorizedActionException("Teacher is not actively assigned to this student's class.");
            }
        }
        // STUDENT role check for self-access is handled by PreAuthorize in controller.
        // ADMIN/SUPER_ADMIN can view.
        return convertToDTO(student);
    }

    @Override
    public StudentDTO getStudentByUserId(Long userId, User currentUser) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user id: " + userId));

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (student.getClasses() == null || !subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(student.getClasses().getId(), teacherProfile.getId(), "ACTIVE")) {
                throw new UnauthorizedActionException("Teacher is not actively assigned to this student's class.");
            }
        }
        // STUDENT role check for self-access is handled by PreAuthorize in controller.
        // ADMIN/SUPER_ADMIN can view.
        return convertToDTO(student);
    }

    @Override
    public List<StudentDTO> getAllStudents(User currentUser) {
        // This method might be too broad. If teachers access, it should be restricted.
        // For now, let's assume PreAuthorize handles high-level role access (e.g. only Admin/SuperAdmin)
        // Finer-grained filtering for teachers (e.g. only students in their classes) would require more complex logic here
        // or separate dedicated methods.
        // The current requirement is to allow ADMIN/SUPER_ADMIN/TEACHER. Teacher access implies all students they teach.
        if (currentUser.getRole() == UserRole.TEACHER) {
             Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            List<SubjectAssignment> assignments = subjectAssignmentRepository.findByTeacherIdAndStatus(teacherProfile.getId(), "ACTIVE");
            List<Long> classIdsTaughtByTeacher = assignments.stream().map(sa -> sa.getClasses().getId()).distinct().collect(Collectors.toList());

            return studentRepository.findAll().stream()
                    .filter(student -> student.getClasses() != null && classIdsTaughtByTeacher.contains(student.getClasses().getId()))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        return studentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentDTO> getStudentsByClassId(Long classId, User currentUser) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (!subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(classId, teacherProfile.getId(), "ACTIVE")) {
                throw new UnauthorizedActionException("Teacher is not actively assigned to this class.");
            }
        }
        // STUDENT role check for self-access (if they are in this classId) could be added, but usually handled by UI restricting classId choice.
        // ADMIN/SUPER_ADMIN can view.
        return studentRepository.findByClassesId(classId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentDTO updateStudent(Long studentId, UpdateStudentRequestDTO requestDTO) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (requestDTO.getClassId() != null) {
            Classes newClass = classRepository.findById(requestDTO.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("New class not found with id: " + requestDTO.getClassId()));
            student.setClasses(newClass);
        }
        // Other updatable fields for Student entity can be added here

        Student updatedStudent = studentRepository.save(student);
        return convertToDTO(updatedStudent);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        // Consider related entities: tasks? Or are they soft deleted / unlinked?
        studentRepository.deleteById(id);
    }

    private StudentDTO convertToDTO(Student student) {
        StudentDTO studentDTO = modelMapper.map(student, StudentDTO.class);
        if (student.getUser() != null) {
            studentDTO.setUser(modelMapper.map(student.getUser(), UserDTO.class));
        }
        if (student.getClasses() != null) {
            studentDTO.setClassId(student.getClasses().getId());
            studentDTO.setClassName(student.getClasses().getName());
        }
        // Tasks list can be populated if needed, but omitted for now as per DTO design
        return studentDTO;
    }
}
