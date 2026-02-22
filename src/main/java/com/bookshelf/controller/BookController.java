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

import org.springframework.data.domain.Page;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing books in the BookShelf application
 * Handles book upload, retrieval, updates, deletion, and file serving
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookController {

    private final BookService bookService;
    private final PdfProcessingService pdfProcessingService;

    /**
     * Upload a new PDF book to the library
     * Processes PDF, extracts metadata, generates thumbnail, and fetches data from Google Books API
     *
     * @param file PDF file to upload (multipart/form-data)
     * @return BookResponse with book details including metadata
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookResponse> uploadBook(@RequestParam("file") MultipartFile file) {
        BookResponse response = bookService.uploadBook(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all books with optional filtering and sorting
     *
     * @param search Optional search query for title or author (case-insensitive)
     * @param sortBy Optional sort field: 'title', 'dateAdded', 'lastRead', 'progress'
     * @param status Optional filter by reading status: 'UNREAD', 'READING', 'FINISHED'
     * @return List of books matching the criteria
     */
    @GetMapping
    public ResponseEntity<?> getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Page<BookResponse> pagedBooks = bookService.getAllBooksPaged(search, sortBy, status, page, size);
            return ResponseEntity.ok(pagedBooks);
        }
        List<BookResponse> books = bookService.getAllBooks(search, sortBy, status);
        return ResponseEntity.ok(books);
    }

    /**
     * Get library statistics
     * Includes total books count, status breakdown, and continue reading suggestion
     *
     * @return Library statistics with book counts and recently read book
     */
    @GetMapping("/stats")
    public ResponseEntity<LibraryStatsResponse> getLibraryStats() {
        LibraryStatsResponse stats = bookService.getLibraryStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get featured books for the home page
     * Returns recently read books ordered by last read timestamp
     *
     * @param limit Number of books to return (default: 5)
     * @return List of recently read books
     */
    @GetMapping("/featured")
    public ResponseEntity<List<BookResponse>> getFeaturedBooks(
            @RequestParam(defaultValue = "5") int limit) {
        List<BookResponse> featuredBooks = bookService.getFeaturedBooks(limit);
        return ResponseEntity.ok(featuredBooks);
    }

    /**
     * Get a specific book by ID
     *
     * @param id Book UUID
     * @return Book details
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    /**
     * Update book metadata
     * Allows updating title, author, description, genre, status, and cover URL
     *
     * @param id Book UUID
     * @param request Updated book fields
     * @return Updated book details
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable UUID id,
            @RequestBody BookUpdateRequest request) {
        BookResponse response = bookService.updateBook(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update reading progress for a book
     * Automatically updates reading status based on progress
     *
     * @param id Book UUID
     * @param request Current page and optional status
     * @return Updated book details with new progress
     */
    @PutMapping("/{id}/progress")
    public ResponseEntity<BookResponse> updateProgress(
            @PathVariable UUID id,
            @RequestBody ProgressUpdateRequest request) {
        BookResponse response = bookService.updateProgress(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get reading progress for a specific book
     *
     * @param id Book UUID
     * @return Book details including current progress
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<BookResponse> getProgress(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    /**
     * Delete a book and all associated data
     * Removes PDF file, thumbnail, cached audio, and database records
     *
     * @param id Book UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Serve the PDF file for a book
     * Returns PDF with inline disposition for browser viewing
     *
     * @param id Book UUID
     * @return PDF file as application/pdf
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getPdf(@PathVariable UUID id) {
        BookResponse book = bookService.getBookById(id);
        String pdfPath = bookService.getPdfPath(id);
        File pdfFile = pdfProcessingService.getPdfFile(pdfPath);

        Resource resource = new FileSystemResource(pdfFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + book.getTitle() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /**
     * Serve the thumbnail image for a book
     * Returns ultra-high-quality PNG thumbnail generated from first page at 600 DPI
     *
     * @param id Book UUID
     * @return Thumbnail image as image/png
     */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID id) {
        String thumbnailPath = bookService.getThumbnailPath(id);
        File thumbnailFile = pdfProcessingService.getThumbnailFile(thumbnailPath);

        Resource resource = new FileSystemResource(thumbnailFile);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
