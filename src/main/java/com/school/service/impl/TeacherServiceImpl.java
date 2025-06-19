package com.school.service.impl;

import com.school.dto.CreateTeacherRequestDTO;
import com.school.dto.TeacherDTO;
import com.school.dto.UpdateTeacherRequestDTO;
import com.school.dto.UserDTO; // Required for mapping
import com.school.entity.Teacher;
import com.school.entity.User;
import com.school.entity.UserRole;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.TeacherRepository;
import com.school.repository.UserRepository;
import com.school.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public TeacherDTO createTeacher(CreateTeacherRequestDTO requestDTO) {
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestDTO.getUserId()));

        // Ensure user has a TEACHER role or set it.
        if (user.getRole() != UserRole.TEACHER) {
            // This could be an error, or an automatic role assignment
            // For now, let's assume an error if user is not already a teacher type
            throw new IllegalArgumentException("User with ID " + requestDTO.getUserId() + " does not have the TEACHER role.");
        }

        if (teacherRepository.existsByUserId(requestDTO.getUserId())) {
             throw new IllegalArgumentException("User with ID " + requestDTO.getUserId() + " is already registered as a teacher.");
        }

        Teacher teacher = Teacher.builder()
                .user(user)
                .subjects(requestDTO.getSubjects() != null ? new ArrayList<>(requestDTO.getSubjects()) : new ArrayList<>())
                .build();

        Teacher savedTeacher = teacherRepository.save(teacher);
        return convertToDTO(savedTeacher);
    }

    @Override
    public TeacherDTO getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
        return convertToDTO(teacher);
    }

    @Override
    public TeacherDTO getTeacherByUserId(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user id: " + userId));
        return convertToDTO(teacher);
    }

    @Override
    public List<TeacherDTO> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherDTO updateTeacher(Long teacherId, UpdateTeacherRequestDTO requestDTO) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        if (requestDTO.getSubjects() != null) {
            teacher.setSubjects(new ArrayList<>(requestDTO.getSubjects()));
        }
        // Other updatable fields for Teacher entity can be added here

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return convertToDTO(updatedTeacher);
    }

    @Override
    @Transactional
    public void deleteTeacher(Long id) {
        if (!teacherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Teacher not found with id: " + id);
        }
        // Consider related entities: assigned tasks, class teacher roles?
        teacherRepository.deleteById(id);
    }

    private TeacherDTO convertToDTO(Teacher teacher) {
        TeacherDTO teacherDTO = modelMapper.map(teacher, TeacherDTO.class);
        if (teacher.getUser() != null) {
            teacherDTO.setUser(modelMapper.map(teacher.getUser(), UserDTO.class));
        }
        // assignedTasks list can be populated if needed, but omitted for now
        return teacherDTO;
    }
}
