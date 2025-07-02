package com.school.service.impl;

import com.school.dto.SubjectRequestDTO;
import com.school.dto.SubjectResponseDTO;
import com.school.entity.Subject;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.SubjectRepository;
import com.school.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public SubjectResponseDTO createSubject(SubjectRequestDTO requestDTO) {
        if (subjectRepository.existsByName(requestDTO.getName())) {
            throw new IllegalArgumentException("Subject with name '" + requestDTO.getName() + "' already exists.");
        }
        if (requestDTO.getSubjectCode() != null && subjectRepository.existsBySubjectCode(requestDTO.getSubjectCode())) {
            throw new IllegalArgumentException("Subject with code '" + requestDTO.getSubjectCode() + "' already exists.");
        }

        Subject subject = modelMapper.map(requestDTO, Subject.class);
        Subject savedSubject = subjectRepository.save(subject);
        return modelMapper.map(savedSubject, SubjectResponseDTO.class);
    }

    @Override
    public SubjectResponseDTO getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        return modelMapper.map(subject, SubjectResponseDTO.class);
    }

    @Override
    public SubjectResponseDTO getSubjectByCode(String subjectCode) {
        Subject subject = subjectRepository.findBySubjectCode(subjectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with code: " + subjectCode));
        return modelMapper.map(subject, SubjectResponseDTO.class);
    }

    @Override
    public List<SubjectResponseDTO> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(subject -> modelMapper.map(subject, SubjectResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        // Check for name conflict if name is being changed
        if (requestDTO.getName() != null && !subject.getName().equals(requestDTO.getName()) && subjectRepository.existsByName(requestDTO.getName())) {
            throw new IllegalArgumentException("Another subject with name '" + requestDTO.getName() + "' already exists.");
        }
        // Check for code conflict if code is being changed or added
        if (requestDTO.getSubjectCode() != null && !requestDTO.getSubjectCode().equals(subject.getSubjectCode()) && subjectRepository.existsBySubjectCode(requestDTO.getSubjectCode())) {
             throw new IllegalArgumentException("Another subject with code '" + requestDTO.getSubjectCode() + "' already exists.");
        }


        if(requestDTO.getName() != null) subject.setName(requestDTO.getName());
        if(requestDTO.getSubjectCode() != null) subject.setSubjectCode(requestDTO.getSubjectCode());
        // else if subjectCode in DTO is null, it could mean unsetting it if business rule allows
        // For now, only update if provided. To make it nullable, DDL and this logic would need adjustment.

        Subject updatedSubject = subjectRepository.save(subject);
        return modelMapper.map(updatedSubject, SubjectResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with id: " + id);
        }
        // Consider implications: if subject is assigned to classes?
        // Add check or rely on DB foreign key constraints (e.g., ON DELETE RESTRICT for subject_assignments.subject_id)
        subjectRepository.deleteById(id);
    }
}
