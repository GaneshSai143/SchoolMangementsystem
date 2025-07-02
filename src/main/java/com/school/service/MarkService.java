package com.school.service;

import com.school.dto.EnterMarkRequestDTO;
import com.school.dto.MarkResponseDTO;
import com.school.entity.User;
import java.util.List;

public interface MarkService {
    MarkResponseDTO enterMarks(EnterMarkRequestDTO requestDTO, User currentUser);
    MarkResponseDTO updateMarks(Long markId, EnterMarkRequestDTO requestDTO, User currentUser);
    MarkResponseDTO getMarkById(Long markId, User currentUser);
    List<MarkResponseDTO> getMarksByStudentId(Long studentId, User currentUser); // Student Profile ID
    List<MarkResponseDTO> getMarksByStudentUserId(Long studentUserId, User currentUser); // Main User ID
    List<MarkResponseDTO> getMarksBySubjectAssignmentId(Long subjectAssignmentId, User currentUser);
    void deleteMark(Long markId, User currentUser);
}
