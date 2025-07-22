package com.school.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaChatRequest {
    private String model;
    private java.util.List<ChatMessage> messages;
    private boolean stream;
    private Map<String, Object> options;
}
