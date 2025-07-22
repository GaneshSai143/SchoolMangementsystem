package com.school.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.school.dto.AiPromptRequest;
import com.school.dto.AiPromptResponse;
import com.school.service.impl.OllamaService;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "Ollama Mistral AI Integration")
public class OllamaController {

    private final OllamaService ollamaService;

    @PostMapping("/generate")
    @Operation(summary = "Generate AI response", description = "Generate response using Ollama Mistral model")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<AiPromptResponse> generateResponse(@Valid @RequestBody AiPromptRequest request) {
        log.info("Generating AI response for prompt: {}", request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));
        
        AiPromptResponse response = ollamaService.generateResponse(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI", description = "Interactive chat with Ollama Mistral model")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<AiPromptResponse> chatWithAI(
            @RequestParam @NotBlank String message,
            @RequestParam(required = false) String systemMessage,
            @RequestParam(required = false) String model) {
        
        log.info("Chat request: {}", message.substring(0, Math.min(50, message.length())));
        
        AiPromptResponse response = ollamaService.chatResponse(message, systemMessage, model);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Check AI service health", description = "Check if Ollama service is running")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean isRunning = ollamaService.isOllamaReachable();
        
        return ResponseEntity.ok(Map.of(
                "status", isRunning ? "UP" : "DOWN",
                "service", "Ollama Mistral",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/models")
    @Operation(summary = "Get available models", description = "Get list of available Ollama models")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<String>> getAvailableModels() {
        return ollamaService.getAvailableModels()
                .map(models -> ResponseEntity.ok(models))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error fetching models"));
    }

    // School-specific AI endpoints
    
    @PostMapping("/generate-lesson-plan")
    @Operation(summary = "Generate lesson plan", description = "Generate lesson plan using AI")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<AiPromptResponse> generateLessonPlan(
            @RequestParam @NotBlank String subject,
            @RequestParam @NotBlank String topic,
            @RequestParam @NotBlank String gradeLevel,
            @RequestParam(required = false, defaultValue = "45") int durationMinutes) {
        
        String systemMessage = "You are an experienced teacher and curriculum designer. Create detailed, engaging lesson plans.";
        String prompt = String.format(
                "Create a comprehensive lesson plan for:\n" +
                "Subject: %s\n" +
                "Topic: %s\n" +
                "Grade Level: %s\n" +
                "Duration: %d minutes\n\n" +
                "Include: Learning objectives, materials needed, lesson structure, activities, and assessment methods.",
                subject, topic, gradeLevel, durationMinutes
        );

        AiPromptRequest request = AiPromptRequest.builder()
                .prompt(prompt)
                .systemMessage(systemMessage)
                .build();

        return generateResponse(request);
    }

    @PostMapping("/explain-concept")
    @Operation(summary = "Explain educational concept", description = "Get AI explanation of educational concepts")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<AiPromptResponse> explainConcept(
            @RequestParam @NotBlank String concept,
            @RequestParam @NotBlank String subject,
            @RequestParam(required = false, defaultValue = "high school") String level) {
        
        String systemMessage = "You are a knowledgeable tutor. Explain concepts clearly and provide examples suitable for the specified education level.";
        String prompt = String.format(
                "Explain the concept of '%s' in %s for %s level students. " +
                "Provide clear explanations, examples, and practical applications.",
                concept, subject, level
        );

        AiPromptRequest request = AiPromptRequest.builder()
                .prompt(prompt)
                .systemMessage(systemMessage)
                .build();

        return generateResponse(request);
    }

    @PostMapping("/generate-quiz")
    @Operation(summary = "Generate quiz questions", description = "Generate quiz questions using AI")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<AiPromptResponse> generateQuiz(
            @RequestParam @NotBlank String topic,
            @RequestParam @NotBlank String subject,
            @RequestParam(required = false, defaultValue = "5") int questionCount,
            @RequestParam(required = false, defaultValue = "multiple choice") String questionType) {
        
        String systemMessage = "You are an expert in creating educational assessments. Generate clear, well-structured questions with appropriate difficulty.";
        String prompt = String.format(
                "Create %d %s questions about '%s' in %s. " +
                "Include questions with varying difficulty levels. " +
                "For multiple choice questions, provide 4 options with the correct answer clearly marked.",
                questionCount, questionType, topic, subject
        );

        AiPromptRequest request = AiPromptRequest.builder()
                .prompt(prompt)
                .systemMessage(systemMessage)
                .build();

        return generateResponse(request);
    }
}
