package com.school.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON
public class ErrorResponseDTO {
    private LocalDateTime timestamp;
    private int status;
    private String error; // e.g., "Unauthorized", "Not Found"
    private String message; // Developer/user-friendly message
    private String path; // URI of the request
    private List<String> details; // For multiple validation errors, etc.
    private Map<String, String> fieldErrors; // For field-specific validation errors
}
