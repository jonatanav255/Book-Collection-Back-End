package com.bookshelf.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for GlobalExceptionHandler.
 * No Spring context needed — just plain instantiation.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── ResourceNotFoundException → 404 ───────────────────────────────────────

    @Test
    void handleResourceNotFound_returns404WithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Book not found with id: abc");

        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
        assertThat(response.getBody().get("message")).isEqualTo("Book not found with id: abc");
    }

    @Test
    void handleResourceNotFound_includesTimestamp() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("not found"));

        assertThat(response.getBody()).containsKey("timestamp");
    }

    // ── DuplicateBookException → 409 ──────────────────────────────────────────

    @Test
    void handleDuplicateBook_returns409WithMessage() {
        DuplicateBookException ex = new DuplicateBookException("Already exists: My Book");

        ResponseEntity<Map<String, Object>> response = handler.handleDuplicateBook(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("error", "Conflict");
        assertThat(response.getBody().get("message")).isEqualTo("Already exists: My Book");
    }

    // ── PdfProcessingException → 400 ──────────────────────────────────────────

    @Test
    void handlePdfProcessing_returns400() {
        PdfProcessingException ex = new PdfProcessingException("Corrupt PDF");

        ResponseEntity<Map<String, Object>> response = handler.handlePdfProcessing(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "PDF Processing Error");
    }

    // ── MaxUploadSizeExceededException → 413 ──────────────────────────────────

    @Test
    void handleMaxUploadSize_returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024L);

        ResponseEntity<Map<String, Object>> response = handler.handleMaxUploadSize(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("status", 413);
        assertThat(response.getBody().get("message")).isEqualTo("File size exceeds maximum allowed limit");
    }

    // ── MethodArgumentNotValidException → 400 with field errors ───────────────

    @Test
    void handleValidationErrors_returns400WithFieldErrors() throws Exception {
        // Build a real BindingResult with a field error
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "noteCreateRequest");
        bindingResult.addError(new FieldError("noteCreateRequest", "content", "Content is required"));

        // MethodArgumentNotValidException requires a MethodParameter — use a constructor via reflection
        java.lang.reflect.Method method = Object.class.getDeclaredMethod("toString");
        org.springframework.core.MethodParameter methodParam = new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParam, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Validation Error");

        @SuppressWarnings("unchecked")
        Map<String, String> validationErrors = (Map<String, String>) response.getBody().get("validationErrors");
        assertThat(validationErrors).containsEntry("content", "Content is required");
    }

    // ── DataIntegrityViolationException → 409 ────────────────────────────────

    @Test
    void handleDataIntegrity_fileHash_returns409WithDuplicateMessage() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "could not execute statement; SQL [n/a]; constraint [file_hash]");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody().get("message"))
                .isEqualTo("A book with the same content already exists");
    }

    @Test
    void handleDataIntegrity_generic_returns409WithGenericMessage() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException("constraint violation");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message")).isEqualTo("A data conflict occurred");
    }

    // ── Generic Exception → 500 ───────────────────────────────────────────────

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Something broke");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred");
    }

    // ── Sanity: all responses have JSON Content-Type ──────────────────────────

    @Test
    void allHandlers_returnJsonContentType() {
        var r1 = handler.handleResourceNotFound(new ResourceNotFoundException("x"));
        var r2 = handler.handleDuplicateBook(new DuplicateBookException("x"));
        var r3 = handler.handlePdfProcessing(new PdfProcessingException("x"));
        var r4 = handler.handleGenericException(new Exception("x"));

        for (var r : List.of(r1, r2, r3, r4)) {
            assertThat(r.getHeaders().getContentType())
                    .isNotNull()
                    .hasToString("application/json");
        }
    }
}
