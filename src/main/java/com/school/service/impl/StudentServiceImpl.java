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
import com.school.repository.ClassRepository;
import com.school.repository.StudentRepository;
import com.school.repository.UserRepository;
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
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return convertToDTO(student);
    }

    @Override
    public StudentDTO getStudentByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user id: " + userId));
        return convertToDTO(student);
    }

    @Override
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentDTO> getStudentsByClassId(Long classId) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }
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
