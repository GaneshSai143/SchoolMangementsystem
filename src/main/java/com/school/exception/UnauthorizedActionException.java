package com.school.exception;

// import org.springframework.http.HttpStatus; // No longer needed here
// import org.springframework.web.bind.annotation.ResponseStatus; // No longer needed here

// @ResponseStatus(HttpStatus.FORBIDDEN) // Removed for centralized handling
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
