package com.school.service;

import com.school.dto.CreateTeacherRequestDTO;
import com.school.dto.TeacherDTO;
import com.school.dto.UpdateTeacherRequestDTO;
import java.util.List;

public interface TeacherService {
    TeacherDTO createTeacher(CreateTeacherRequestDTO requestDTO);
    TeacherDTO getTeacherById(Long id);
    TeacherDTO getTeacherByUserId(Long userId);
    List<TeacherDTO> getAllTeachers();
    TeacherDTO updateTeacher(Long teacherId, UpdateTeacherRequestDTO requestDTO);
    void deleteTeacher(Long id);
}
