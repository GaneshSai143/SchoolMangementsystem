package com.school.controller;

import com.school.dto.CreateSchoolRequestDTO;
import com.school.dto.SchoolDTO;
import com.school.dto.UpdateSchoolRequestDTO;
import com.school.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
// @Tag will be added in a later step for Swagger
public class SchoolController {

    private final SchoolService schoolService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SchoolDTO> createSchool(@Valid @RequestBody CreateSchoolRequestDTO requestDTO) {
        SchoolDTO createdSchool = schoolService.createSchool(requestDTO);
        return new ResponseEntity<>(createdSchool, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<SchoolDTO> getSchoolById(@PathVariable Long id) {
        SchoolDTO school = schoolService.getSchoolById(id);
        return ResponseEntity.ok(school);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<SchoolDTO>> getAllSchools() {
        List<SchoolDTO> schools = schoolService.getAllSchools();
        return ResponseEntity.ok(schools);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SchoolDTO> updateSchool(@PathVariable Long id, @Valid @RequestBody UpdateSchoolRequestDTO requestDTO) {
        SchoolDTO updatedSchool = schoolService.updateSchool(id, requestDTO);
        return ResponseEntity.ok(updatedSchool);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteSchool(@PathVariable Long id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.noContent().build();
    }
}
