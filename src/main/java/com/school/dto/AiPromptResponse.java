package com.school.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptResponse {
    private String response;
    private String model;
    private LocalDateTime timestamp;
    private Long responseTimeMs;
    private boolean success;
    private String errorMessage;
}
