package com.bookshelf.controller;

import com.bookshelf.dto.*;
import com.bookshelf.service.BookService;
import com.bookshelf.service.PdfProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookController {

    private final BookService bookService;
    private final PdfProcessingService pdfProcessingService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookResponse> uploadBook(@RequestParam("file") MultipartFile file) {
        BookResponse response = bookService.uploadBook(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String status) {
        List<BookResponse> books = bookService.getAllBooks(search, sortBy, status);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/stats")
    public ResponseEntity<LibraryStatsResponse> getLibraryStats() {
        LibraryStatsResponse stats = bookService.getLibraryStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable UUID id,
            @RequestBody BookUpdateRequest request) {
        BookResponse response = bookService.updateBook(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<BookResponse> updateProgress(
            @PathVariable UUID id,
            @RequestBody ProgressUpdateRequest request) {
        BookResponse response = bookService.updateProgress(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<BookResponse> getProgress(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getPdf(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        File pdfFile = pdfProcessingService.getPdfFile(book.getFileHash() != null ?
            "/data/bookshelf/pdfs/" + id + ".pdf" : "");

        Resource resource = new FileSystemResource(pdfFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + book.getTitle() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        File thumbnailFile = pdfProcessingService.getThumbnailFile(
            "/data/bookshelf/thumbnails/" + id + ".jpg");

        Resource resource = new FileSystemResource(thumbnailFile);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
