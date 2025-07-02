package com.school.service;

import com.school.dto.CreateStudentRequestDTO;
import com.school.dto.StudentDTO;
import com.school.dto.UpdateStudentRequestDTO;
import com.school.entity.User;
import java.util.List;

public interface StudentService {
    StudentDTO createStudent(CreateStudentRequestDTO requestDTO,User user); // Typically by Admin/SuperAdmin
    StudentDTO getStudentById(Long id, User currentUser);
    StudentDTO getStudentByUserId(Long userId, User currentUser);
    List<StudentDTO> getAllStudents(User currentUser); // Might be restricted further
    List<StudentDTO> getStudentsByClassId(Long classId, User currentUser);
    StudentDTO updateStudent(Long studentId, UpdateStudentRequestDTO requestDTO, User user); // Typically by Admin/SuperAdmin
    void deleteStudent(Long id, User user); // Typically by Admin/SuperAdmin
}
