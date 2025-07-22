package com.school.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.school.dto.AiPromptRequest;
import com.school.dto.AiPromptResponse;
import com.school.dto.ChatMessage;
import com.school.dto.OllamaChatRequest;
import com.school.dto.OllamaChatResponse;
import com.school.dto.OllamaGenerateRequest;
import com.school.dto.OllamaGenerateResponse;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final WebClient ollamaWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.default-model:llama2:7b}")
    private String defaultModel;

    @Value("${ollama.timeout:60}")
    private int timeoutSeconds;

    @Value("${ollama.max-retries:2}")
    private int maxRetries;

    /**
     * Generate response with fixed JSON handling
     */
    public AiPromptResponse generateResponse(AiPromptRequest request) {
        long startTime = System.currentTimeMillis();
        String model = request.getModel() != null ? request.getModel() : defaultModel;
        
        log.info("=== OLLAMA REQUEST START ===");
        log.info("Model: {}", model);
        log.info("Prompt length: {}", request.getPrompt().length());
        log.info("Timeout: {}s", timeoutSeconds);
        
        // Test connectivity first
        if (!isOllamaReachable()) {
            return createErrorResponse(model, startTime, 
                "Ollama service is not reachable. Please check if Ollama is running with: ollama serve");
        }
        
        try {
            // Create request with explicit stream = false
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", request.getPrompt());
            requestBody.put("stream", false);  // Explicitly set to false
            requestBody.put("options", getOptimizedOptions());
            
            if (request.getSystemMessage() != null && !request.getSystemMessage().trim().isEmpty()) {
                requestBody.put("system", request.getSystemMessage());
            }

            log.info("Request body: {}", requestBody);

            // Get response as raw string first, then parse manually
            String rawResponse = ollamaWebClient
                    .post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(2))
                            .filter(throwable -> {
                                log.warn("Retrying due to: {}", throwable.getMessage());
                                return throwable instanceof java.util.concurrent.TimeoutException || 
                                       throwable instanceof WebClientRequestException;
                            }))
                    .doOnSuccess(response -> log.info("Raw response received from Ollama"))
                    .doOnError(error -> log.error("Ollama request failed: {}", error.getMessage()))
                    .block();

            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                throw new RuntimeException("Received null or empty response from Ollama");
            }

            log.info("Raw response: {}", rawResponse.substring(0, Math.min(200, rawResponse.length())));

            // Parse the response manually
            String responseText = parseOllamaResponse(rawResponse);
            
            if (responseText == null || responseText.trim().isEmpty()) {
                throw new RuntimeException("Could not extract response text from Ollama response");
            }

            long responseTime = System.currentTimeMillis() - startTime;
            log.info("Response generated successfully in {}ms", responseTime);

            return AiPromptResponse.builder()
                    .response(responseText)
                    .model(model)
                    .timestamp(LocalDateTime.now())
                    .responseTimeMs(responseTime)
                    .success(true)
                    .build();

        } catch (WebClientResponseException e) {
            log.error("Ollama API HTTP error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // Special handling for model not found
            if (e.getStatusCode().value() == 404) {
                return createErrorResponse(model, startTime, 
                    String.format("Model '%s' not found. Please run: ollama pull %s", model, model));
            }
            
            return createErrorResponse(model, startTime, 
                String.format("Ollama API error: %s - %s", e.getStatusCode(), e.getResponseBodyAsString()));
        } catch (WebClientRequestException e) {
            log.error("Connection error to Ollama: {}", e.getMessage());
            return createErrorResponse(model, startTime, 
                "Cannot connect to Ollama service. Please ensure Ollama is running and accessible.");
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return createErrorResponse(model, startTime, 
                "Service error: " + e.getMessage());
        }
    }

    /**
     * Parse Ollama response handling both single object and array formats
     */
    private String parseOllamaResponse(String rawResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawResponse);
            
            // Handle array response (streaming format)
            if (jsonNode.isArray()) {
                log.info("Received array response, processing...");
                StringBuilder responseBuilder = new StringBuilder();
                
                for (JsonNode item : jsonNode) {
                    if (item.has("response")) {
                        responseBuilder.append(item.get("response").asText());
                    }
                }
                
                return responseBuilder.toString();
            }
            // Handle single object response (non-streaming format)
            else if (jsonNode.has("response")) {
                return jsonNode.get("response").asText();
            }
            // Handle error response
            else if (jsonNode.has("error")) {
                throw new RuntimeException("Ollama error: " + jsonNode.get("error").asText());
            }
            else {
                log.warn("Unexpected response format: {}", rawResponse);
                return rawResponse; // Return raw response as fallback
            }
            
        } catch (Exception e) {
            log.error("Error parsing Ollama response: {}", e.getMessage());
            // Try to extract any readable text
            return rawResponse.contains("response") ? 
                rawResponse.substring(rawResponse.indexOf("response") + 10) : 
                "Error parsing response: " + e.getMessage();
        }
    }

    /**
     * Chat response with proper JSON handling
     */
    public AiPromptResponse chatResponse(String userMessage, String systemMessage, String model) {
        long startTime = System.currentTimeMillis();
        String selectedModel = model != null ? model : defaultModel;
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", selectedModel);
            requestBody.put("stream", false);
            requestBody.put("options", getOptimizedOptions());
            
            // Build messages array
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemMessage != null ? systemMessage : "You are a helpful assistant."),
                Map.of("role", "user", "content", userMessage)
            );
            requestBody.put("messages", messages);

            log.info("Sending chat request to model: {}", selectedModel);

            String rawResponse = ollamaWebClient
                    .post()
                    .uri("/api/chat")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(2))
                            .filter(throwable -> throwable instanceof java.util.concurrent.TimeoutException))
                    .block();

            String responseText = parseChatResponse(rawResponse);
            long responseTime = System.currentTimeMillis() - startTime;

            return AiPromptResponse.builder()
                    .response(responseText)
                    .model(selectedModel)
                    .timestamp(LocalDateTime.now())
                    .responseTimeMs(responseTime)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error in chat response: {}", e.getMessage());
            return createErrorResponse(model, startTime, "Chat error: " + e.getMessage());
        }
    }

    /**
     * Parse chat response from Ollama
     */
    private String parseChatResponse(String rawResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawResponse);
            
            // Handle array response
            if (jsonNode.isArray()) {
                for (JsonNode item : jsonNode) {
                    if (item.has("message") && item.get("message").has("content")) {
                        return item.get("message").get("content").asText();
                    }
                }
            }
            // Handle single object response
            else if (jsonNode.has("message") && jsonNode.get("message").has("content")) {
                return jsonNode.get("message").get("content").asText();
            }
            
            return "No valid response found";
            
        } catch (Exception e) {
            log.error("Error parsing chat response: {}", e.getMessage());
            return "Error parsing response: " + e.getMessage();
        }
    }

    /**
     * Test connection with simple request
     */
    public AiPromptResponse testConnection() {
        log.info("=== TESTING OLLAMA CONNECTION ===");
        
        AiPromptRequest testRequest = AiPromptRequest.builder()
                .prompt("Say 'Hello'")
                .model(defaultModel)
                .build();
        
        return generateResponse(testRequest);
    }

    /**
     * Check if Ollama service is running
     */
    public boolean isOllamaReachable() {
        try {
            String response = ollamaWebClient
                    .get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            log.info("Ollama connectivity test: SUCCESS");
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.warn("Ollama connectivity test: FAILED - {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get available models
     */
    public Mono<String> getAvailableModels() {
        return ollamaWebClient
                .get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> log.error("Failed to fetch models: {}", error.getMessage()))
                .onErrorReturn("Error fetching models: ");
    }

    /**
     * Optimized options for better performance
     */
    private Map<String, Object> getOptimizedOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("top_p", 0.9);
        options.put("top_k", 40);
        options.put("num_predict", 300);     // Reasonable length
        options.put("num_ctx", 2048);        // Context window
        options.put("repeat_penalty", 1.1);
        options.put("stop", List.of("\n\n"));
        return options;
    }

    private AiPromptResponse createErrorResponse(String model, long startTime, String errorMessage) {
        long responseTime = System.currentTimeMillis() - startTime;
        return AiPromptResponse.builder()
                .response("")
                .model(model != null ? model : defaultModel)
                .timestamp(LocalDateTime.now())
                .responseTimeMs(responseTime)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}

