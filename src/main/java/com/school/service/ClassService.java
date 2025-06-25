package com.school.service;

import com.school.dto.ClassDTO;
import com.school.dto.ClassDTO;
import com.school.dto.CreateClassRequestDTO;
import com.school.dto.UpdateClassRequestDTO;
import com.school.entity.User;
import java.util.List;

public interface ClassService {
    ClassDTO createClass(CreateClassRequestDTO requestDTO, User currentUser);
    ClassDTO getClassById(Long id, User currentUser);
    List<ClassDTO> getAllClasses(User currentUser);
    List<ClassDTO> getClassesBySchoolId(Long schoolId, User currentUser);
    ClassDTO updateClass(Long id, UpdateClassRequestDTO requestDTO, User currentUser);
    void deleteClass(Long id, User currentUser);
    ClassDTO assignClassTeacher(Long classId, Long teacherId, User currentUser);
}
