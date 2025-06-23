package com.school.service;

import com.school.dto.UserDTO; // Re-using UserDTO for principal response
import com.school.dto.TeacherDTO; // Re-using TeacherDTO for teacher response
import com.school.dto.StudentDTO; // Re-using StudentDTO for student response
import com.school.dto.CreatePrincipalRequestDTO;
import com.school.dto.CreateTeacherByAdminRequestDTO;
import com.school.dto.CreateStudentByAdminRequestDTO;
import com.school.entity.User; // Added for loggedInAdmin parameter type

public interface AdminService {
    UserDTO createPrincipal(CreatePrincipalRequestDTO requestDTO);
    TeacherDTO createTeacherByAdmin(CreateTeacherByAdminRequestDTO requestDTO, User loggedInAdmin);
    StudentDTO createStudentByAdmin(CreateStudentByAdminRequestDTO requestDTO, User loggedInAdmin);
}
