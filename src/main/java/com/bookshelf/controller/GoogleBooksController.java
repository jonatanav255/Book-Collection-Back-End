package com.bookshelf.controller;

import com.bookshelf.dto.GoogleBooksResponse;
import com.bookshelf.service.GoogleBooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books/lookup")
@RequiredArgsConstructor
public class GoogleBooksController {

    private final GoogleBooksService googleBooksService;

    @GetMapping
    public ResponseEntity<GoogleBooksResponse> searchBooks(@RequestParam String query) {
        GoogleBooksResponse response = googleBooksService.searchBooks(query);
        return ResponseEntity.ok(response);
    }
}
