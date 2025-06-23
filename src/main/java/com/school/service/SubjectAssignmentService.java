package com.school.service;

import com.school.dto.SubjectAssignmentRequestDTO;
import com.school.dto.SubjectAssignmentResponseDTO;
import com.school.entity.User;
import java.util.List;

public interface SubjectAssignmentService {
    SubjectAssignmentResponseDTO createAssignment(SubjectAssignmentRequestDTO requestDTO, User currentUser);
    SubjectAssignmentResponseDTO getAssignmentById(Long id);
    List<SubjectAssignmentResponseDTO> getAssignmentsByClassId(Long classId);
    List<SubjectAssignmentResponseDTO> getAssignmentsByTeacherId(Long teacherId);
    List<SubjectAssignmentResponseDTO> getAssignmentsBySubjectId(Long subjectId);
    SubjectAssignmentResponseDTO updateAssignmentStatus(Long id, String status, User currentUser);
    // More specific updates can be added, e.g., updateTeacherForAssignment
    void deleteAssignment(Long id, User currentUser);
}
