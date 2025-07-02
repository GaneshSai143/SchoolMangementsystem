package com.school.exception;

import com.school.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException; // Spring Security's base
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.List; // Added for List.of


@ControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponseDTO.ErrorResponseDTOBuilder createErrorBuilder(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.NOT_FOUND, ex.getMessage(), request).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedActionException.class) // Custom 403 type
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedActionException(UnauthorizedActionException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.FORBIDDEN, ex.getMessage(), request).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CustomAuthenticationException.class) // Custom 401 type
    public ResponseEntity<ErrorResponseDTO> handleCustomAuthenticationException(CustomAuthenticationException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // Spring Security specific exceptions
    @ExceptionHandler(AccessDeniedException.class) // Standard Spring Security 403
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to perform this action.", request)
                                                .details(List.of(ex.getMessage())) // Include original message in details
                                                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class) // For login failures
    public ResponseEntity<ErrorResponseDTO> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.UNAUTHORIZED, "Invalid credentials provided.", request)
                                                 .details(List.of(ex.getMessage()))
                                                 .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class) // Non-authenticated user accessing protected resource
    public ResponseEntity<ErrorResponseDTO> handleInsufficientAuthenticationException(InsufficientAuthenticationException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.UNAUTHORIZED, "Authentication required. Please login.", request)
                                                .details(List.of(ex.getMessage()))
                                                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class) // General Spring Security authentication failures
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
         ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.UNAUTHORIZED, "Authentication failed.", request)
                                                 .details(List.of(ex.getMessage()))
                                                 .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // Spring MVC validation exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                    fieldError -> fieldError.getField(),
                    fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                ));
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.BAD_REQUEST, "Validation failed. Please check your input.", request)
                .fieldErrors(fieldErrors)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.BAD_REQUEST, "Malformed request body. Please check the JSON structure.", request)
                .details(List.of(ex.getMessage()))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class) // For general invalid arguments
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.BAD_REQUEST, ex.getMessage(), request).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Generic fallback handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllOtherExceptions(Exception ex, HttpServletRequest request) {
        // Log the exception for server-side analysis
        // logger.error("An unexpected error occurred: ", ex);
        ErrorResponseDTO errorResponse = createErrorBuilder(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred. Please try again later.", request)
                                                .details(List.of(ex.getClass().getName() + ": " + ex.getMessage())) // Avoid exposing too much detail in production
                                                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
