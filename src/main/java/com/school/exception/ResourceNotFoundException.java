package com.school.exception;

// import org.springframework.http.HttpStatus; // No longer needed here
// import org.springframework.web.bind.annotation.ResponseStatus; // No longer needed here

// @ResponseStatus(HttpStatus.NOT_FOUND) // Removed for centralized handling
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
} 