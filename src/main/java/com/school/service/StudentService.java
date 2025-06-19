package com.school.service;

import com.school.dto.CreateStudentRequestDTO;
import com.school.dto.StudentDTO;
import com.school.dto.UpdateStudentRequestDTO;
import java.util.List;

public interface StudentService {
    StudentDTO createStudent(CreateStudentRequestDTO requestDTO);
    StudentDTO getStudentById(Long id);
    StudentDTO getStudentByUserId(Long userId);
    List<StudentDTO> getAllStudents();
    List<StudentDTO> getStudentsByClassId(Long classId);
    StudentDTO updateStudent(Long studentId, UpdateStudentRequestDTO requestDTO);
    void deleteStudent(Long id);
}
