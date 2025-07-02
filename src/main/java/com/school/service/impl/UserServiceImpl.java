package com.school.service.impl;

import com.school.dto.UserDTO;
import com.school.entity.User;
import com.school.entity.UserRole;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException; // Added
import com.school.repository.UserRepository;
import com.school.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .role(userDTO.getRole() != null ? userDTO.getRole() : UserRole.STUDENT)
                .phoneNumber(userDTO.getPhoneNumber())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO, User currentUser) {
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // If ADMIN, check they are updating a user in their own school
        // SUPER_ADMIN can update anyone. Users can update themselves (handled by PreAuthorize).
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null) {
                throw new UnauthorizedActionException("Principal (Admin) is not associated with a school.");
            }
            if (userToUpdate.getSchoolId() == null || !userToUpdate.getSchoolId().equals(currentUser.getSchoolId())) {
                 throw new UnauthorizedActionException("Principal (Admin) can only update users within their own school.");
            }
        }
        // Note: UserDTO does not contain schoolId or role, so user cannot change these via this method.
        // Password can be changed by self or admin.
        // For self-update, PreAuthorize ensures it's their own ID.

        userToUpdate.setFirstName(userDTO.getFirstName());
        userToUpdate.setLastName(userDTO.getLastName());
        userToUpdate.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            userToUpdate.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(userToUpdate);
        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, User currentUser) {
        // Controller PreAuthorize restricts this to SUPER_ADMIN only.
        // If ADMIN were allowed, school scoping check would be needed:
        // User userToDelete = userRepository.findById(id)
        //         .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        // if (currentUser.getRole() == UserRole.ADMIN) {
        //     if (currentUser.getSchoolId() == null) {
        //         throw new UnauthorizedActionException("Principal (Admin) is not associated with a school.");
        //     }
        //     if (userToDelete.getSchoolId() == null || !userToDelete.getSchoolId().equals(currentUser.getSchoolId())) {
        //         throw new UnauthorizedActionException("Principal (Admin) can only delete users within their own school.");
        //     }
        // }
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // UserDTO getUserById(Long id) remains unchanged, access controlled by PreAuthorize or higher service layers.
    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO processOAuth2User(String email, String name, String provider) {
        String[] nameParts = name.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return userRepository.findByEmail(email)
                .map(user -> {
                    // Update existing user
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setAuthProvider(provider);
                    return convertToDTO(userRepository.save(user));
                })
                .orElseGet(() -> {
                    // Create new user
                    User newUser = User.builder()
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .role(UserRole.STUDENT)
                            .enabled(true)
                            .authProvider(provider)
                            .build();
                    return convertToDTO(userRepository.save(newUser));
                });
    }

    @Override
    public UserDTO getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUserByEmail(email);
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String role, User currentUser) {
        // Controller PreAuthorize restricts this to SUPER_ADMIN only.
        // If ADMIN were allowed, school scoping check would be needed.
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userToUpdate.setRole(UserRole.valueOf(role.toUpperCase()));
        userRepository.save(userToUpdate);
    }

    @Override
    @Transactional
    public void enableUser(Long userId, User currentUser) {
        User userToEnable = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null) {
                throw new UnauthorizedActionException("Principal (Admin) is not associated with a school.");
            }
            if (userToEnable.getSchoolId() == null || !userToEnable.getSchoolId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only enable users within their own school.");
            }
        }
        // SUPER_ADMIN can enable anyone.
        userToEnable.setEnabled(true);
        userRepository.save(userToEnable);
    }

    @Override
    @Transactional
    public void disableUser(Long userId, User currentUser) {
        User userToDisable = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() == null) {
                throw new UnauthorizedActionException("Principal (Admin) is not associated with a school.");
            }
            if (userToDisable.getSchoolId() == null || !userToDisable.getSchoolId().equals(currentUser.getSchoolId())) {
                throw new UnauthorizedActionException("Principal (Admin) can only disable users within their own school.");
            }
        }
        // SUPER_ADMIN can disable anyone.
        userToDisable.setEnabled(false);
        userRepository.save(userToDisable);
    }


    @Override
    public List<UserDTO> getUsersByRole(UserRole role) {
        UserDTO currentUserDTO = getCurrentUser();
        User currentUser = userRepository.findByEmail(currentUserDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUserDTO.getEmail()));

        List<User> users;
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            users = userRepository.findAllByRole(role);
        } else if (currentUser.getRole() == UserRole.ADMIN) {
            if (role == UserRole.PRINCIPAL) {
                // Admins are not allowed to list all principals.
                throw new AccessDeniedException("Admins are not allowed to list all principals.");
            }
            // Admins can only list users within their own school
            if (currentUser.getSchoolId() == null) {
                throw new AccessDeniedException("Admin is not associated with any school.");
            }
            users = userRepository.findAllByRoleAndSchoolId(role, currentUser.getSchoolId());
        } else {
            // Other roles (TEACHER, STUDENT) are not allowed to list users by role
            throw new AccessDeniedException("You do not have permission to access this resource.");
        }
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setSchoolId(user.getSchoolId()); // Ensure schoolId is mapped
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setPreferredTheme(user.getPreferredTheme()); // Add this line
        return dto;
    }

    @Override
    @Transactional
    public UserDTO updateUserTheme(String userEmail, String theme) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
        user.setPreferredTheme(theme);
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }
} 
