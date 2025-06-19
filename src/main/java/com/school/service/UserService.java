package com.school.service;

import com.school.dto.UserDTO;
import com.school.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

public interface UserService {
    UserDTO createUser(UserDTO userDTO);
    UserDTO updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    UserDTO getUserById(Long id);
    UserDTO getUserByEmail(String email);
    UserDTO processOAuth2User(String email, String name, String provider);
    UserDTO getCurrentUser();
    void updateUserRole(Long userId, String role);
    void enableUser(Long userId);
    void disableUser(Long userId);
} 