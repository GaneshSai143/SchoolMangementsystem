package com.school.service;

import com.school.dto.UserDTO;
import com.school.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

public interface UserService {
    UserDTO createUser(UserDTO userDTO); // Consider if this needs currentUser for school scoping if used by ADMIN
    UserDTO updateUser(Long id, UserDTO userDTO, User currentUser);
    void deleteUser(Long id, User currentUser);
    UserDTO getUserById(Long id); // Read-only, PreAuthorize likely sufficient, or add currentUser if fine-grained needed
    UserDTO getUserByEmail(String email);
    UserDTO processOAuth2User(String email, String name, String provider);
    UserDTO getCurrentUser();
    void updateUserRole(Long userId, String role, User currentUser);
    void enableUser(Long userId, User currentUser);
    void disableUser(Long userId, User currentUser);
    UserDTO updateUserTheme(String userEmail, String theme); // Self-operation, email is key
} 