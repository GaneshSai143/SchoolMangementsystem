package com.school.service.impl;

import com.school.dto.*; // UserDTO, StudentDTO needed
import com.school.entity.Classes;
import com.school.entity.School;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.ClassRepository;
import com.school.repository.SchoolRepository;
import com.school.repository.UserRepository;
import com.school.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ClassDTO createClass(CreateClassRequestDTO requestDTO) {
        School school = schoolRepository.findById(requestDTO.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + requestDTO.getSchoolId()));

        User classTeacher = null;
        if (requestDTO.getClassTeacherId() != null) {
            classTeacher = userRepository.findById(requestDTO.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher user not found with id: " + requestDTO.getClassTeacherId()));
            // Add check to ensure user has TEACHER role if necessary
        }

        Classes newClass = Classes.builder()
                .name(requestDTO.getName())
                .school(school)
                .classTeacher(classTeacher)
                .build();

        Classes savedClass = classRepository.save(newClass);
        return convertToDTO(savedClass);
    }

    @Override
    public ClassDTO getClassById(Long id) {
        Classes foundClass = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
        return convertToDTO(foundClass);
    }

    @Override
    public List<ClassDTO> getAllClasses() {
         return classRepository.findAll().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
    }

    @Override
    public List<ClassDTO> getClassesBySchoolId(Long schoolId) {
        if (!schoolRepository.existsById(schoolId)) {
             throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        return classRepository.findBySchoolId(schoolId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassDTO updateClass(Long id, UpdateClassRequestDTO requestDTO) {
        Classes classToUpdate = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        if (requestDTO.getName() != null) {
            classToUpdate.setName(requestDTO.getName());
        }
        if (requestDTO.getClassTeacherId() != null) {
            User classTeacher = userRepository.findById(requestDTO.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher user not found with id: " + requestDTO.getClassTeacherId()));
            classToUpdate.setClassTeacher(classTeacher);
        } else { // Allow unsetting teacher
            classToUpdate.setClassTeacher(null);
        }

        Classes updatedClass = classRepository.save(classToUpdate);
        return convertToDTO(updatedClass);
    }

    @Override
    @Transactional
    public ClassDTO assignClassTeacher(Long classId, Long teacherId) {
        Classes classToUpdate = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher (User) not found with id: " + teacherId));
        // Add validation: Ensure user is a Teacher (role check)
        // Ensure teacher is part of the same school? (More complex logic)

        classToUpdate.setClassTeacher(teacher);
        Classes updatedClass = classRepository.save(classToUpdate);
        return convertToDTO(updatedClass);
    }


    @Override
    @Transactional
    public void deleteClass(Long id) {
        if (!classRepository.existsById(id)) {
            throw new ResourceNotFoundException("Class not found with id: " + id);
        }
        // Consider implications: students in this class?
        classRepository.deleteById(id);
    }

    private ClassDTO convertToDTO(Classes cls) {
        ClassDTO classDTO = modelMapper.map(cls, ClassDTO.class);
        if (cls.getSchool() != null) {
            classDTO.setSchoolId(cls.getSchool().getId());
        }
        if (cls.getClassTeacher() != null) {
            classDTO.setClassTeacher(modelMapper.map(cls.getClassTeacher(), UserDTO.class));
        }
        // Students list mapping:
        if (cls.getStudents() != null) {
            classDTO.setStudents(cls.getStudents().stream()
                                          .map(student -> modelMapper.map(student, StudentDTO.class))
                                          .collect(Collectors.toList()));
        } else {
            classDTO.setStudents(Collections.emptyList());
        }
        return classDTO;
    }
}
