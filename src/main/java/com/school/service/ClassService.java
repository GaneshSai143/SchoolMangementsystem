package com.school.service;

import com.school.dto.ClassDTO;
import com.school.dto.CreateClassRequestDTO;
import com.school.dto.UpdateClassRequestDTO;
import java.util.List;

public interface ClassService {
    ClassDTO createClass(CreateClassRequestDTO requestDTO);
    ClassDTO getClassById(Long id);
    List<ClassDTO> getAllClasses(); // Or getClassesBySchoolId(Long schoolId)
    List<ClassDTO> getClassesBySchoolId(Long schoolId);
    ClassDTO updateClass(Long id, UpdateClassRequestDTO requestDTO);
    void deleteClass(Long id);
    ClassDTO assignClassTeacher(Long classId, Long teacherId);
}
