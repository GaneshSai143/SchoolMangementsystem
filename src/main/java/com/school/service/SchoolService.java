package com.school.service;

import com.school.dto.SchoolDTO;
import com.school.dto.CreateSchoolRequestDTO;
import com.school.dto.UpdateSchoolRequestDTO;
import java.util.List;

public interface SchoolService {
    SchoolDTO createSchool(CreateSchoolRequestDTO requestDTO);
    SchoolDTO getSchoolById(Long id);
    List<SchoolDTO> getAllSchools();
    SchoolDTO updateSchool(Long id, UpdateSchoolRequestDTO requestDTO);
    void deleteSchool(Long id);
}
