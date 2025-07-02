package com.school.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.dto.UserDTO;
import com.school.entity.User;
import com.school.entity.UserRole;
import com.school.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "superadmin@test.com", roles = {"SUPER_ADMIN"})
    void getUsersByRole_SuperAdmin_shouldReturnOk() throws Exception {
        UserDTO teacherDto = new UserDTO();
        teacherDto.setEmail("teacher@test.com");
        teacherDto.setRole(UserRole.TEACHER);
        List<UserDTO> users = Collections.singletonList(teacherDto);

        when(userService.getUsersByRole(UserRole.TEACHER)).thenReturn(users);

        mockMvc.perform(get("/users/by-role")
                        .param("role", "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("teacher@test.com")));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void getUsersByRole_Admin_AccessingTeachers_shouldReturnOk() throws Exception {
        UserDTO teacherDto = new UserDTO();
        teacherDto.setEmail("teacher@test.com");
        teacherDto.setRole(UserRole.TEACHER);
        teacherDto.setSchoolId(1L); // Assuming admin is associated with school 1
        List<UserDTO> users = Collections.singletonList(teacherDto);

        when(userService.getUsersByRole(UserRole.TEACHER)).thenReturn(users);

        mockMvc.perform(get("/users/by-role")
                        .param("role", "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("teacher@test.com")));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void getUsersByRole_Admin_AccessingPrincipals_shouldReturnForbidden() throws Exception {
        when(userService.getUsersByRole(UserRole.PRINCIPAL))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Admins are not allowed to list all principals."));

        mockMvc.perform(get("/users/by-role")
                        .param("role", "PRINCIPAL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher@test.com", roles = {"TEACHER"})
    void getUsersByRole_Teacher_shouldReturnForbidden() throws Exception {
        // Teachers should not be able to list users by role directly via this endpoint
        // The @PreAuthorize on controller should prevent this if not ADMIN or SUPER_ADMIN
        // Or the service layer will throw AccessDenied if the PreAuthorize is more permissive
        when(userService.getUsersByRole(UserRole.STUDENT))
               .thenThrow(new org.springframework.security.access.AccessDeniedException("You do not have permission to access this resource."));


        mockMvc.perform(get("/users/by-role")
                        .param("role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
