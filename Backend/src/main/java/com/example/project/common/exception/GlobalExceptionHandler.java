package com.example.project.common.exception;

import com.example.project.common.response.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.project.github.client.GitHubApiException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_STATE", exception.getMessage(), request);
    }

    @ExceptionHandler(GitHubReauthorizationRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleGitHubReauthorizationRequired(
            GitHubReauthorizationRequiredException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "GITHUB_REAUTHORIZATION_REQUIRED",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ApiErrorResponse> handleGitHubApi(
            GitHubApiException exception, HttpServletRequest request) {
        HttpStatus status = exception.getStatusCode() == 429 || exception.getStatusCode() == 403
                ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        return error(status, "GITHUB_API_FAILED", exception.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(),
                code,
                message,
                request.getRequestURI()));
    }
}
