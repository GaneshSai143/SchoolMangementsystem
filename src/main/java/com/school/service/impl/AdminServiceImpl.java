package com.school.service.impl;

import com.school.dto.*;
import com.school.entity.*;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.*;
import com.school.service.AdminService;
// UserService is not directly needed here if UserServiceImpl is injected
// import com.school.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList; // For teacher subjects

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    // Assuming UserServiceImpl has a public method to convert User to UserDTO
    // If not, we might need to duplicate or make it a shared utility
    private final UserServiceImpl userServiceImpl; // For User -> UserDTO conversion

    @Override
    @Transactional
    public UserDTO createPrincipal(CreatePrincipalRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + requestDTO.getEmail());
        }
        School school = schoolRepository.findById(requestDTO.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + requestDTO.getSchoolId()));

        User user = User.builder()
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .email(requestDTO.getEmail())
                .password(passwordEncoder.encode(requestDTO.getPassword()))
                .phoneNumber(requestDTO.getPhoneNumber())
                .role(UserRole.ADMIN) // Principal is ADMIN
                .schoolId(school.getId()) // Link principal to the school
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);
        return userServiceImpl.convertToDTO(savedUser); // Re-use UserServiceImpl's converter
    }

    @Override
    @Transactional
    public TeacherDTO createTeacherByAdmin(CreateTeacherByAdminRequestDTO requestDTO, User loggedInAdmin) {
        if (loggedInAdmin.getSchoolId() == null) {
             throw new IllegalArgumentException("Admin is not associated with a school.");
        }
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + requestDTO.getEmail());
        }

        User user = User.builder()
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .email(requestDTO.getEmail())
                .password(passwordEncoder.encode(requestDTO.getPassword()))
                .phoneNumber(requestDTO.getPhoneNumber())
                .role(UserRole.TEACHER)
                .schoolId(loggedInAdmin.getSchoolId()) // Teacher belongs to admin's school
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        Teacher teacherProfile = Teacher.builder()
                .user(savedUser)
                .subjects(requestDTO.getSubjects() != null ? new ArrayList<>(requestDTO.getSubjects()) : new ArrayList<>())
                .build();
        Teacher savedTeacherProfile = teacherRepository.save(teacherProfile);

        // Convert Teacher entity to TeacherDTO
        TeacherDTO teacherDTO = modelMapper.map(savedTeacherProfile, TeacherDTO.class);
        teacherDTO.setUser(userServiceImpl.convertToDTO(savedUser)); // set UserDTO part
        return teacherDTO;
    }

    @Override
    @Transactional
    public StudentDTO createStudentByAdmin(CreateStudentByAdminRequestDTO requestDTO, User loggedInAdmin) {
         if (loggedInAdmin.getSchoolId() == null) {
             throw new IllegalArgumentException("Admin is not associated with a school.");
        }
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + requestDTO.getEmail());
        }

        Classes studentClass = classRepository.findById(requestDTO.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + requestDTO.getClassId()));

        if (!studentClass.getSchool().getId().equals(loggedInAdmin.getSchoolId())) {
            throw new IllegalArgumentException("Class does not belong to the admin's school.");
        }

        User user = User.builder()
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .email(requestDTO.getEmail())
                .password(passwordEncoder.encode(requestDTO.getPassword()))
                .phoneNumber(requestDTO.getPhoneNumber())
                .role(UserRole.STUDENT)
                .schoolId(loggedInAdmin.getSchoolId()) // Student belongs to admin's school
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        Student studentProfile = Student.builder()
                .user(savedUser)
                .classes(studentClass)
                .build();
        Student savedStudentProfile = studentRepository.save(studentProfile);

        // Convert Student entity to StudentDTO
        StudentDTO studentDTO = modelMapper.map(savedStudentProfile, StudentDTO.class);
        studentDTO.setUser(userServiceImpl.convertToDTO(savedUser)); // set UserDTO part
        if (savedStudentProfile.getClasses() != null) {
            studentDTO.setClassName(savedStudentProfile.getClasses().getName()); // Set class name
        }
        return studentDTO;
    }
}
