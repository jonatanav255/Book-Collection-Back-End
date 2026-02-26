package com.bookshelf.controller;

import com.bookshelf.dto.GoogleBooksResponse;
import com.bookshelf.service.GoogleBooksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books/lookup")
@Tag(name = "Google Books", description = "Search the Google Books API for book metadata")
public class GoogleBooksController {

    private final GoogleBooksService googleBooksService;

    public GoogleBooksController(GoogleBooksService googleBooksService) {
        this.googleBooksService = googleBooksService;
    }

    @Operation(summary = "Search Google Books")
    @GetMapping
    public ResponseEntity<GoogleBooksResponse> searchBooks(@RequestParam String query) {
        GoogleBooksResponse response = googleBooksService.searchBooks(query);
        return ResponseEntity.ok(response);
    }
}
