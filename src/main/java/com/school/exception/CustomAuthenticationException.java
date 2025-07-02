package com.school.exception;

// Can extend org.springframework.security.core.AuthenticationException if needed
// to be caught by Spring Security specific mechanisms, or RuntimeException for general use.
// For @ControllerAdvice, RuntimeException is fine.
public class CustomAuthenticationException extends RuntimeException {
    public CustomAuthenticationException(String message) {
        super(message);
    }
}
