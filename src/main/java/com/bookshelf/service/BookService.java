package com.bookshelf.service;

import com.bookshelf.dto.BookResponse;
import com.bookshelf.dto.BookUpdateRequest;
import com.bookshelf.dto.LibraryStatsResponse;
import com.bookshelf.dto.ProgressUpdateRequest;
import com.bookshelf.exception.DuplicateBookException;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for book management operations
 * Coordinates PDF processing, metadata enrichment, and CRUD operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final PdfProcessingService pdfProcessingService;
    private final GoogleBooksService googleBooksService;
    private final NoteService noteService;
    private final TextToSpeechService textToSpeechService;

    /**
     * Upload and process a new book
     * 1. Validates PDF file
     * 2. Extracts PDF metadata and generates thumbnail
     * 3. Checks for duplicates using file hash
     * 4. If duplicate exists in database: rejects upload
     * 5. Fetches enriched metadata from Google Books API
     * 6. Saves book to database
     *
     * Smart reconnection: Uses deterministic book ID from file hash
     * This allows deleted books to reconnect to their existing audio files
     *
     * @param file Uploaded PDF file
     * @return BookResponse with complete book metadata
     * @throws IllegalArgumentException if file is empty or not a PDF
     * @throws DuplicateBookException if identical book already exists in database
     */
    @Transactional
    public BookResponse uploadBook(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        UUID tempId = UUID.randomUUID();

        Map<String, Object> pdfMetadata;
        String fileHash;
        UUID bookId;

        try {
            pdfMetadata = pdfProcessingService.processPdf(file, tempId);
            fileHash = (String) pdfMetadata.get("fileHash");
            bookId = generateDeterministicUUID(fileHash);

            Optional<Book> existingBook = bookRepository.findByFileHash(fileHash);
            if (existingBook.isPresent()) {
                pdfProcessingService.deleteFiles(
                        (String) pdfMetadata.get("pdfPath"),
                        (String) pdfMetadata.get("thumbnailPath")
                );
                throw new DuplicateBookException("A book with the same content already exists: " + existingBook.get().getTitle());
            }

            renameBookFiles(tempId, bookId, pdfMetadata);
        } catch (Exception e) {
            throw e;
        }

        String title = (String) pdfMetadata.getOrDefault("title", originalFilename.replaceFirst("[.][^.]+$", ""));
        String author = (String) pdfMetadata.get("author");

        Map<String, Object> googleMetadata = googleBooksService.fetchMetadata(title, author);

        Book book = Book.builder()
                .id(bookId)
                .title((String) googleMetadata.getOrDefault("title", title))
                .author((String) googleMetadata.getOrDefault("author", author))
                .description((String) googleMetadata.get("description"))
                .genre((String) googleMetadata.get("genre"))
                .pageCount((Integer) pdfMetadata.get("pageCount"))
                .currentPage(0)
                .status(ReadingStatus.UNREAD)
                .pdfPath((String) pdfMetadata.get("pdfPath"))
                .thumbnailPath((String) pdfMetadata.get("thumbnailPath"))
                .coverUrl((String) googleMetadata.get("coverUrl"))
                .fileHash(fileHash)
                .dateAdded(LocalDateTime.now())
                .build();

        book = bookRepository.save(book);

        return mapToResponse(book);
    }

    public List<BookResponse> getAllBooks(String search, String sortBy, String status) {
        List<Book> books;

        if (search != null && !search.trim().isEmpty()) {
            books = bookRepository.searchBooks(search.trim());
        } else if (status != null && !status.trim().isEmpty()) {
            ReadingStatus readingStatus = ReadingStatus.valueOf(status.toUpperCase());
            if (sortBy != null && !sortBy.isEmpty()) {
                books = bookRepository.findByStatusSorted(readingStatus, sortBy);
            } else {
                books = bookRepository.findByStatus(readingStatus);
            }
        } else if (sortBy != null && !sortBy.isEmpty()) {
            books = bookRepository.findAllSorted(sortBy);
        } else {
            books = bookRepository.findAll();
        }

        return books.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<BookResponse> getAllBooksPaged(String search, String sortBy, String status, int page, int size) {
        Sort sort = buildSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();

        Page<Book> books;

        if (hasSearch && hasStatus) {
            ReadingStatus readingStatus = ReadingStatus.valueOf(status.toUpperCase());
            books = bookRepository.searchBooksByStatus(search.trim(), readingStatus, pageable);
        } else if (hasSearch) {
            books = bookRepository.searchBooks(search.trim(), pageable);
        } else if (hasStatus) {
            ReadingStatus readingStatus = ReadingStatus.valueOf(status.toUpperCase());
            books = bookRepository.findByStatus(readingStatus, pageable);
        } else {
            books = bookRepository.findAll(pageable);
        }

        return books.map(this::mapToResponse);
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "dateAdded");
        }
        return switch (sortBy) {
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            case "lastRead" -> Sort.by(Sort.Direction.DESC, "lastReadAt");
            case "progress" -> Sort.by(Sort.Direction.DESC, "progressRatio");
            default -> Sort.by(Sort.Direction.DESC, "dateAdded");
        };
    }

    public BookResponse getBookById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return mapToResponse(book);
    }

    @Transactional
    public BookResponse updateBook(UUID id, BookUpdateRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getGenre() != null) {
            book.setGenre(request.getGenre());
        }
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }
        if (request.getCoverUrl() != null) {
            book.setCoverUrl(request.getCoverUrl());
        }
        if (request.getCurrentPage() != null) {
            book.setCurrentPage(request.getCurrentPage());
        }

        book = bookRepository.save(book);

        return mapToResponse(book);
    }

    @Transactional
    public BookResponse updateProgress(UUID id, ProgressUpdateRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (request.getCurrentPage() != null) {
            book.setCurrentPage(request.getCurrentPage());
        }

        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        } else {
            // Auto-update status based on page
            if (book.getCurrentPage() > 0 && book.getStatus() == ReadingStatus.UNREAD) {
                book.setStatus(ReadingStatus.READING);
            }
            if (book.getCurrentPage() != null && book.getPageCount() != null) {
                if (book.getCurrentPage() >= book.getPageCount()) {
                    book.setStatus(ReadingStatus.FINISHED);
                } else if (book.getStatus() == ReadingStatus.FINISHED) {
                    book.setStatus(ReadingStatus.READING);
                }
            }
        }

        // Update last read timestamp
        book.setLastReadAt(LocalDateTime.now());

        book = bookRepository.save(book);

        return mapToResponse(book);
    }

    /**
     * Delete a book from the library
     * NOTE: Audio files are preserved to avoid wasting Google TTS costs
     * You can manually delete audio later if needed via DELETE /api/books/{id}/audio
     *
     * @param id Book UUID
     */
    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        // Delete associated notes
        noteService.deleteNotesByBookId(id);

        // Delete files from filesystem (PDF and thumbnail only)
        pdfProcessingService.deleteFiles(book.getPdfPath(), book.getThumbnailPath());

        // KEEP cached audio files - they cost money to generate via Google TTS
        // Audio can be deleted manually via: DELETE /api/books/{id}/audio

        // Delete database record
        bookRepository.delete(book);
    }

    public LibraryStatsResponse getLibraryStats() {
        long totalBooks = bookRepository.count();
        long unreadBooks = bookRepository.countByStatus(ReadingStatus.UNREAD);
        long readingBooks = bookRepository.countByStatus(ReadingStatus.READING);
        long finishedBooks = bookRepository.countByStatus(ReadingStatus.FINISHED);
        long totalPages = bookRepository.sumTotalPages();
        long totalPagesRead = bookRepository.sumTotalPagesRead();

        BookResponse continueReading = bookRepository.findMostRecentlyRead()
                .map(this::mapToResponse)
                .orElse(null);

        return LibraryStatsResponse.builder()
                .totalBooks(totalBooks)
                .unreadBooks(unreadBooks)
                .readingBooks(readingBooks)
                .finishedBooks(finishedBooks)
                .totalPages(totalPages)
                .totalPagesRead(totalPagesRead)
                .continueReading(continueReading)
                .build();
    }

    public List<BookResponse> getFeaturedBooks(int limit) {
        List<Book> recentlyReadBooks = bookRepository.findRecentlyReadBooks(limit);

        return recentlyReadBooks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public String getPdfPath(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        return book.getPdfPath();
    }

    public String getThumbnailPath(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        return book.getThumbnailPath();
    }

    /**
     * Regenerate all thumbnails as optimized JPEGs
     * Converts old 600 DPI PNG thumbnails (~3-46MB each) to 150 DPI JPEG (~30-80KB each)
     */
    @Transactional
    public int regenerateAllThumbnails() {
        List<Book> allBooks = bookRepository.findAll();
        int count = 0;
        for (Book book : allBooks) {
            try {
                if (book.getPdfPath() != null) {
                    String newPath = pdfProcessingService.regenerateThumbnail(book.getPdfPath(), book.getId());
                    book.setThumbnailPath(newPath);
                    bookRepository.save(book);
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to regenerate thumbnail for book {}: {}", book.getId(), e.getMessage());
            }
        }
        log.info("Regenerated {} thumbnails", count);
        return count;
    }

    /**
     * Generate deterministic UUID from file hash
     * Same PDF file will always generate the same book ID
     * This enables reconnecting to existing audio files after re-upload
     *
     * @param fileHash SHA-256 hash of the PDF file
     * @return Deterministic UUID based on the hash
     */
    private UUID generateDeterministicUUID(String fileHash) {
        try {
            // Use first 32 characters of hash to create UUID
            // Format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            String formatted = fileHash.substring(0, 8) + "-" +
                    fileHash.substring(8, 12) + "-" +
                    fileHash.substring(12, 16) + "-" +
                    fileHash.substring(16, 20) + "-" +
                    fileHash.substring(20, 32);
            return UUID.fromString(formatted);
        } catch (Exception e) {
            return UUID.randomUUID(); // Fallback to random
        }
    }

    /**
     * Rename PDF and thumbnail files from temporary ID to final deterministic ID
     *
     * @param tempId Temporary UUID used during initial processing
     * @param finalId Final deterministic UUID based on file hash
     * @param metadata Metadata map containing file paths to update
     */
    private void renameBookFiles(UUID tempId, UUID finalId, Map<String, Object> metadata) {
        try {
            String oldPdfPath = (String) metadata.get("pdfPath");
            String oldThumbnailPath = (String) metadata.get("thumbnailPath");

            // Build new paths by replacing just the filename
            java.nio.file.Path oldPdfFile = java.nio.file.Paths.get(oldPdfPath);
            java.nio.file.Path oldThumbnailFile = java.nio.file.Paths.get(oldThumbnailPath);

            String newPdfFilename = finalId.toString() + ".pdf";
            String newThumbnailFilename = finalId.toString() + ".png";

            java.nio.file.Path newPdfFile = oldPdfFile.getParent().resolve(newPdfFilename);
            java.nio.file.Path newThumbnailFile = oldThumbnailFile.getParent().resolve(newThumbnailFilename);

            // Rename PDF file
            java.nio.file.Files.move(
                    oldPdfFile,
                    newPdfFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            // Rename thumbnail file
            java.nio.file.Files.move(
                    oldThumbnailFile,
                    newThumbnailFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            // Update metadata map with new paths
            metadata.put("pdfPath", newPdfFile.toString());
            metadata.put("thumbnailPath", newThumbnailFile.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename book files: " + e.getMessage(), e);
        }
    }

    private BookResponse mapToResponse(Book book) {
        double progressPercentage = 0.0;
        if (book.getPageCount() != null && book.getPageCount() > 0 && book.getCurrentPage() != null) {
            progressPercentage = (double) book.getCurrentPage() / book.getPageCount() * 100.0;
        }

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .description(book.getDescription())
                .genre(book.getGenre())
                .pageCount(book.getPageCount())
                .currentPage(book.getCurrentPage())
                .status(book.getStatus())
                .coverUrl(book.getCoverUrl())
                .fileHash(book.getFileHash())
                .dateAdded(book.getDateAdded())
                .lastReadAt(book.getLastReadAt())
                .progressPercentage(progressPercentage)
                .build();
    }
}
