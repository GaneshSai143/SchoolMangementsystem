package com.school.service.impl;

import com.school.dto.*; // UserDTO, StudentDTO needed
import com.school.entity.*;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException;
import com.school.repository.*;
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
    private final TeacherRepository teacherRepository; // Added
    private final SubjectAssignmentRepository subjectAssignmentRepository; // Added
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ClassDTO createClass(CreateClassRequestDTO requestDTO, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) { // Principal
            if (currentUser.getSchoolId() == null) {
                throw new IllegalArgumentException("Principal (Admin) is not associated with any school.");
            }
            if (!currentUser.getSchoolId().equals(requestDTO.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only create classes for their own school.");
            }
        }
        // SUPER_ADMIN can create for any school, so no check for them here.
        // For other roles (if any were allowed), they'd need specific checks.

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
    public ClassDTO getClassById(Long id, User currentUser) {
        Classes foundClass = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (!subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(id, teacherProfile.getId(), "ACTIVE")) {
                throw new UnauthorizedActionException("Teacher is not actively assigned to this class.");
            }
        }
        // ADMIN/SUPER_ADMIN can view any class subject to PreAuthorize in controller
        return convertToDTO(foundClass);
    }

    @Override
    public List<ClassDTO> getAllClasses(User currentUser) {
        List<Classes> allClasses = classRepository.findAll();
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            List<SubjectAssignment> assignments = subjectAssignmentRepository.findByTeacherIdAndStatus(teacherProfile.getId(), "ACTIVE");
            List<Long> classIdsTaughtByTeacher = assignments.stream().map(sa -> sa.getClasses().getId()).distinct().collect(Collectors.toList());
            allClasses = allClasses.stream().filter(cls -> classIdsTaughtByTeacher.contains(cls.getId())).collect(Collectors.toList());
        }
        return allClasses.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ClassDTO> getClassesBySchoolId(Long schoolId, User currentUser) {
        if (!schoolRepository.existsById(schoolId)) {
             throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        List<Classes> classesInSchool = classRepository.findBySchoolId(schoolId);

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            // Further ensure teacher belongs to this school if necessary, though assignments check is primary
            if(teacherProfile.getUser().getSchoolId() == null || !teacherProfile.getUser().getSchoolId().equals(schoolId)){
                 throw new UnauthorizedActionException("Teacher does not belong to this school.");
            }
            List<SubjectAssignment> assignments = subjectAssignmentRepository.findByTeacherIdAndStatus(teacherProfile.getId(), "ACTIVE");
            List<Long> classIdsTaughtByTeacher = assignments.stream()
                                                    .filter(sa -> sa.getClasses().getSchool().getId().equals(schoolId)) // ensure class is in the requested school
                                                    .map(sa -> sa.getClasses().getId())
                                                    .distinct()
                                                    .collect(Collectors.toList());
            classesInSchool = classesInSchool.stream().filter(cls -> classIdsTaughtByTeacher.contains(cls.getId())).collect(Collectors.toList());
        }
        return classesInSchool.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassDTO updateClass(Long id, UpdateClassRequestDTO requestDTO, User currentUser) {
        Classes classToUpdate = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null || !classToUpdate.getSchool().getId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only update classes in their own school.");
            }
        }
        // SUPER_ADMIN can update any class.

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
    public ClassDTO assignClassTeacher(Long classId, Long teacherId, User currentUser) {
        Classes classToUpdate = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        User teacherUser = userRepository.findById(teacherId) // Renamed to avoid conflict
                .orElseThrow(() -> new ResourceNotFoundException("Teacher (User) not found with id: " + teacherId));

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null) {
                throw new UnauthorizedActionException("Principal (Admin) is not associated with a school.");
            }
            if (!classToUpdate.getSchool().getId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only assign teachers for classes in their own school.");
            }
            if (teacherUser.getSchoolId() == null || !teacherUser.getSchoolId().equals(currentUser.getSchoolId())) {
                 throw new UnauthorizedActionException("Teacher does not belong to the Principal's school.");
            }
        }
        // SUPER_ADMIN can assign any teacher to any class.
        // Add validation: Ensure user is a Teacher (role check)
        if (teacherUser.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("User with ID " + teacherId + " is not a TEACHER and cannot be assigned as class teacher.");
        }


        classToUpdate.setClassTeacher(teacherUser);
        Classes updatedClass = classRepository.save(classToUpdate);
        return convertToDTO(updatedClass);
    }


    @Override
    @Transactional
    public void deleteClass(Long id, User currentUser) {
        Classes classToDelete = classRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null || !classToDelete.getSchool().getId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only delete classes in their own school.");
            }
        }
        // SUPER_ADMIN can delete any class.

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
