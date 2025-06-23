package com.school.service;

import com.school.dto.SubjectRequestDTO;
import com.school.dto.SubjectResponseDTO;
import java.util.List;

public interface SubjectService {
    SubjectResponseDTO createSubject(SubjectRequestDTO requestDTO);
    SubjectResponseDTO getSubjectById(Long id);
    SubjectResponseDTO getSubjectByCode(String subjectCode);
    List<SubjectResponseDTO> getAllSubjects();
    SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO);
    void deleteSubject(Long id);
}
