package com.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.school.entity.UserRole; // Added
import jakarta.validation.constraints.NotNull; // Added

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeacherByAdminRequestDTO {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    private String phoneNumber;

    private List<String> subjects; // List of subject names teacher is proficient in

    @NotNull(message = "Role is required")
    private UserRole role;
}
