package com.school.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaGenerateRequest {
    private String model;
    private String prompt;
    private boolean stream;
    private Map<String, Object> options;
    private String context;
    private String system;
}
