package com.school.service.impl;

import com.school.dto.*; // Includes the DTOs created above
import com.school.entity.*;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException;
import com.school.repository.*;
import com.school.service.SubjectAssignmentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectAssignmentServiceImpl implements SubjectAssignmentService {

    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository; // For Teacher profiles
    private final ModelMapper modelMapper;
    private final UserServiceImpl userServiceImpl; // For User -> UserDTO in nested TeacherDTO

    @Override
    @Transactional
    public SubjectAssignmentResponseDTO createAssignment(SubjectAssignmentRequestDTO requestDTO, User currentUser) {
        Classes cls = classRepository.findById(requestDTO.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + requestDTO.getClassId()));
        Subject subject = subjectRepository.findById(requestDTO.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + requestDTO.getSubjectId()));
        Teacher teacher = teacherRepository.findById(requestDTO.getTeacherId()) // Teacher profile ID
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + requestDTO.getTeacherId()));

        // Authorization: Principal can only assign for their own school
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null) {
                throw new UnauthorizedActionException("Principal (Admin) is not associated with a school.");
            }
            if (!cls.getSchool().getId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only create assignments for classes in their own school.");
            }
            // Also ensure teacher belongs to the same school
            if (teacher.getUser() == null || !currentUser.getSchoolId().equals(teacher.getUser().getSchoolId())) {
                 throw new UnauthorizedActionException("Teacher does not belong to the Principal's school.");
            }
        }
        // SUPER_ADMIN has no school restriction

        SubjectAssignment assignment = SubjectAssignment.builder()
                .classes(cls)
                .subject(subject)
                .teacher(teacher)
                .academicYear(requestDTO.getAcademicYear())
                .term(requestDTO.getTerm())
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus().toUpperCase() : "ACTIVE")
                .build();

        // Check for uniqueness constraint violation before saving (class_id, subject_id, teacher_id, academic_year, term)
        // This is implicitly handled by DB, but a pre-check can give a better error.
        // Example: if (subjectAssignmentRepository.existsByClassesAndSubjectAndTeacherAndAcademicYearAndTerm(...)) throw ...

        SubjectAssignment savedAssignment = subjectAssignmentRepository.save(assignment);
        return convertToResponseDTO(savedAssignment);
    }

    @Override
    public SubjectAssignmentResponseDTO getAssignmentById(Long id) {
        SubjectAssignment assignment = subjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject assignment not found with id: " + id));
        return convertToResponseDTO(assignment);
    }

    @Override
    public List<SubjectAssignmentResponseDTO> getAssignmentsByClassId(Long classId) {
        if (!classRepository.existsById(classId)) throw new ResourceNotFoundException("Class not found: " + classId);
        return subjectAssignmentRepository.findByClassesId(classId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectAssignmentResponseDTO> getAssignmentsByTeacherId(Long teacherId) {
         if (!teacherRepository.existsById(teacherId)) throw new ResourceNotFoundException("Teacher not found: " + teacherId);
        return subjectAssignmentRepository.findByTeacherId(teacherId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectAssignmentResponseDTO> getAssignmentsBySubjectId(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) throw new ResourceNotFoundException("Subject not found: " + subjectId);
        return subjectAssignmentRepository.findBySubjectId(subjectId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectAssignmentResponseDTO updateAssignmentStatus(Long id, String status, User currentUser) {
        SubjectAssignment assignment = subjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject assignment not found with id: " + id));

        // Authorization check: Principal for their school, or SUPER_ADMIN
        if (currentUser.getRole() == UserRole.ADMIN) {
             if (currentUser.getSchoolId() == null || !assignment.getClasses().getSchool().getId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) cannot update this assignment.");
            }
        }

        assignment.setStatus(status.toUpperCase());
        SubjectAssignment updatedAssignment = subjectAssignmentRepository.save(assignment);
        return convertToResponseDTO(updatedAssignment);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long id, User currentUser) {
        SubjectAssignment assignment = subjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject assignment not found with id: " + id));

                 // Authorization check
                if (currentUser.getRole() == UserRole.ADMIN) {
                     if (currentUser.getSchoolId() == null || !assignment.getClasses().getSchool().getId().equals(currentUser.getSchoolId())) {
                        throw new UnauthorizedActionException("Principal (Admin) cannot delete this assignment.");
                    }
                }

        subjectAssignmentRepository.deleteById(id);
    }

    public SubjectAssignmentResponseDTO convertToResponseDTO(SubjectAssignment assignment) {
        SubjectAssignmentResponseDTO dto = modelMapper.map(assignment, SubjectAssignmentResponseDTO.class);
        // ModelMapper should map nested objects if target DTOs are correct
        // Ensure ClassDTO, SubjectResponseDTO, TeacherDTO are correctly populated.
        // TeacherDTO might need special handling for its UserDTO part.
        if (assignment.getClasses() != null) {
            dto.setClassDTO(modelMapper.map(assignment.getClasses(), ClassDTO.class));
            // ClassDTO's own nested DTOs (like teacher, students) might need careful mapping
            // or should be kept shallow for this specific response.
            // For now, assume direct mapping works or ClassDTO is self-contained enough.
        }
        if (assignment.getSubject() != null) {
            dto.setSubjectDTO(modelMapper.map(assignment.getSubject(), SubjectResponseDTO.class));
        }
        if (assignment.getTeacher() != null) {
            TeacherDTO teacherDTO = modelMapper.map(assignment.getTeacher(), TeacherDTO.class);
            if (assignment.getTeacher().getUser() != null) {
                teacherDTO.setUser(userServiceImpl.convertToDTO(assignment.getTeacher().getUser()));
            }
            dto.setTeacherDTO(teacherDTO);
        }
        return dto;
    }
}
