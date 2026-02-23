package com.bookshelf.controller;

import com.bookshelf.dto.GoogleBooksResponse;
import com.bookshelf.service.GoogleBooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books/lookup")
public class GoogleBooksController {

    private final GoogleBooksService googleBooksService;

    public GoogleBooksController(GoogleBooksService googleBooksService) {
        this.googleBooksService = googleBooksService;
    }

    @GetMapping
    public ResponseEntity<GoogleBooksResponse> searchBooks(@RequestParam String query) {
        GoogleBooksResponse response = googleBooksService.searchBooks(query);
        return ResponseEntity.ok(response);
    }
}
