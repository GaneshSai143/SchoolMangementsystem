package com.school.service.impl;

import com.school.dto.CreateSchoolRequestDTO;
import com.school.dto.SchoolDTO;
import com.school.dto.UserDTO; // For mocking principal in SchoolDTO
import com.school.entity.School;
import com.school.entity.User;
import com.school.entity.UserRole;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.SchoolRepository;
import com.school.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolServiceImplTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private UserRepository userRepository;

    @Mock // Use Spy for ModelMapper if you want to test its actual mapping logic for some parts
         // or @Mock if you want to completely mock its behavior.
         // For simple DTO<->Entity, @Spy is often good to ensure mappings work.
         // However, for strict unit tests, @Mock is preferred. Let's use @Mock for stricter isolation.
    private ModelMapper modelMapper;

    @InjectMocks
    private SchoolServiceImpl schoolService;

    private CreateSchoolRequestDTO createSchoolRequestDTO;
    private User principalUser;
    private School school;
    private SchoolDTO schoolDTO;

    @BeforeEach
    void setUp() {
        createSchoolRequestDTO = CreateSchoolRequestDTO.builder()
                .name("Test School")
                .location("Test Location")
                .principalId(1L)
                .build();

        principalUser = User.builder()
                .id(1L)
                .email("principal@example.com")
                .firstName("Principal")
                .lastName("User")
                .role(UserRole.ADMIN) // Assuming ADMIN is a principal role
                .build();

        school = School.builder()
                .id(1L)
                .name("Test School")
                .location("Test Location")
                .principal(principalUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .classes(Collections.emptyList()) // Initialize to avoid NPE during mapping
                .build();

        // SchoolDTO needs its own principal (UserDTO) and classes (List<ClassDTO>)
        UserDTO principalUserDTO = new UserDTO();
        principalUserDTO.setId(principalUser.getId());
        principalUserDTO.setEmail(principalUser.getEmail());

        schoolDTO = new SchoolDTO();
        schoolDTO.setId(school.getId());
        schoolDTO.setName(school.getName());
        schoolDTO.setLocation(school.getLocation());
        schoolDTO.setPrincipal(principalUserDTO); // Simplified for test
        schoolDTO.setClasses(Collections.emptyList());
        schoolDTO.setCreatedAt(school.getCreatedAt());
        schoolDTO.setUpdatedAt(school.getUpdatedAt());
    }

    @Test
    void testCreateSchool_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(principalUser));
        when(schoolRepository.save(any(School.class))).thenReturn(school);

        // Mock ModelMapper conversions
        // For entity to DTO
        when(modelMapper.map(any(School.class), eq(SchoolDTO.class))).thenReturn(schoolDTO);
        // If SchoolDTO's principal mapping is complex or needs specific setup:
        when(modelMapper.map(eq(principalUser), eq(UserDTO.class))).thenReturn(schoolDTO.getPrincipal());


        // Act
        SchoolDTO result = schoolService.createSchool(createSchoolRequestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test School", result.getName());
        assertEquals(1L, result.getPrincipal().getId());
        verify(userRepository, times(1)).findById(1L);
        verify(schoolRepository, times(1)).save(any(School.class));
        verify(modelMapper, times(1)).map(any(School.class), eq(SchoolDTO.class));
    }

    @Test
    void testCreateSchool_PrincipalNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            schoolService.createSchool(createSchoolRequestDTO);
        });

        assertEquals("Principal user not found with id: 1", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(schoolRepository, never()).save(any(School.class));
    }
}
