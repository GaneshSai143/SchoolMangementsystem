package com.school.service.impl;

import com.school.dto.CreateSchoolRequestDTO;
import com.school.dto.SchoolDTO;
import com.school.dto.UpdateSchoolRequestDTO;
import com.school.dto.UserDTO; // Required for mapping
import com.school.dto.ClassDTO; // Required for mapping
import com.school.entity.School;
import com.school.entity.User;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.SchoolRepository;
import com.school.repository.UserRepository;
import com.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper; // Ensure ModelMapper is configured as a bean

    @Override
    @Transactional
    public SchoolDTO createSchool(CreateSchoolRequestDTO requestDTO) {
        User principal = userRepository.findById(requestDTO.getPrincipalId())
                .orElseThrow(() -> new ResourceNotFoundException("Principal user not found with id: " + requestDTO.getPrincipalId()));

        School school = School.builder()
                .name(requestDTO.getName())
                .location(requestDTO.getLocation())
                .principal(principal)
                .build();
        // Note: 'classes' list will be empty initially
        School savedSchool = schoolRepository.save(school);
        return convertToDTO(savedSchool);
    }

    @Override
    public SchoolDTO getSchoolById(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
        return convertToDTO(school);
    }

    @Override
    public List<SchoolDTO> getAllSchools() {
        return schoolRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SchoolDTO updateSchool(Long id, UpdateSchoolRequestDTO requestDTO) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));

        if (requestDTO.getName() != null) {
            school.setName(requestDTO.getName());
        }
        if (requestDTO.getLocation() != null) {
            school.setLocation(requestDTO.getLocation());
        }
        if (requestDTO.getPrincipalId() != null) {
            User principal = userRepository.findById(requestDTO.getPrincipalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Principal user not found with id: " + requestDTO.getPrincipalId()));
            school.setPrincipal(principal);
        }
        School updatedSchool = schoolRepository.save(school);
        return convertToDTO(updatedSchool);
    }

    @Override
    @Transactional
    public void deleteSchool(Long id) {
        if (!schoolRepository.existsById(id)) {
            throw new ResourceNotFoundException("School not found with id: " + id);
        }
        schoolRepository.deleteById(id);
    }

    private SchoolDTO convertToDTO(School school) {
        SchoolDTO schoolDTO = modelMapper.map(school, SchoolDTO.class);
        // Manual mapping for nested DTOs if ModelMapper doesn't handle them correctly out-of-the-box
        // or if custom logic is needed.
        // For UserDTO principal:
        if (school.getPrincipal() != null) {
            schoolDTO.setPrincipal(modelMapper.map(school.getPrincipal(), UserDTO.class));
        }
        // For List<ClassDTO> classes:
        // This could be complex depending on lazy/eager loading and depth.
        // For now, let's assume ModelMapper handles it or it's kept shallow.
        // If classes are lazily loaded and session is closed, this will fail.
        // A common approach is to map IDs or load them explicitly if needed.
        if (school.getClasses() != null) {
             schoolDTO.setClasses(school.getClasses().stream()
                   .map(c -> modelMapper.map(c, ClassDTO.class))
                   .collect(Collectors.toList()));
        } else {
            schoolDTO.setClasses(Collections.emptyList());
        }
        return schoolDTO;
    }
}
