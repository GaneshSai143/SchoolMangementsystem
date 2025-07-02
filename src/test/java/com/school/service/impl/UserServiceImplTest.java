package com.school.service.impl;

import com.school.dto.UserDTO;
import com.school.entity.User;
import com.school.entity.UserRole;
import com.school.exception.ResourceNotFoundException;
import com.school.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User superAdminUser;
    private User adminUser;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        superAdminUser = User.builder().id(1L).email("superadmin@test.com").role(UserRole.SUPER_ADMIN).build();
        adminUser = User.builder().id(2L).email("admin@test.com").role(UserRole.ADMIN).schoolId(1L).build();
        teacherUser = User.builder().id(3L).email("teacher@test.com").role(UserRole.TEACHER).schoolId(1L).build();

        // Mock SecurityContextHolder
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockCurrentUser(User user) {
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void getUsersByRole_SuperAdmin_shouldReturnAllUsersForRole() {
        mockCurrentUser(superAdminUser);
        List<User> teachers = Collections.singletonList(teacherUser);
        when(userRepository.findAllByRole(UserRole.TEACHER)).thenReturn(teachers);

        List<UserDTO> result = userService.getUsersByRole(UserRole.TEACHER);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(teacherUser.getEmail(), result.get(0).getEmail());
        verify(userRepository).findAllByRole(UserRole.TEACHER);
    }

    @Test
    void getUsersByRole_Admin_shouldReturnUsersForSchoolAndRole() {
        mockCurrentUser(adminUser);
        List<User> teachersInSchool = Collections.singletonList(teacherUser);
        when(userRepository.findAllByRoleAndSchoolId(UserRole.TEACHER, adminUser.getSchoolId())).thenReturn(teachersInSchool);

        List<UserDTO> result = userService.getUsersByRole(UserRole.TEACHER);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(teacherUser.getEmail(), result.get(0).getEmail());
        verify(userRepository).findAllByRoleAndSchoolId(UserRole.TEACHER, adminUser.getSchoolId());
    }

    @Test
    void getUsersByRole_Admin_triesToGetPrincipals_shouldThrowAccessDenied() {
        mockCurrentUser(adminUser);

        assertThrows(AccessDeniedException.class, () -> {
            userService.getUsersByRole(UserRole.PRINCIPAL);
        });
    }

    @Test
    void getUsersByRole_Teacher_triesToGetUsers_shouldThrowAccessDenied() {
        mockCurrentUser(teacherUser);

        assertThrows(AccessDeniedException.class, () -> {
            userService.getUsersByRole(UserRole.STUDENT);
        });
    }

    @Test
    void getUsersByRole_AdminNotAssociatedWithSchool_shouldThrowAccessDenied() {
        User adminWithoutSchool = User.builder().id(4L).email("adminnoschool@test.com").role(UserRole.ADMIN).schoolId(null).build();
        mockCurrentUser(adminWithoutSchool);

        assertThrows(AccessDeniedException.class, () -> {
            userService.getUsersByRole(UserRole.TEACHER);
        });
    }
}
