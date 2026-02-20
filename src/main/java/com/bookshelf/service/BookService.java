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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final PdfProcessingService pdfProcessingService;
    private final GoogleBooksService googleBooksService;
    private final NoteService noteService;
    private final TextToSpeechService textToSpeechService;

    @Transactional
    public BookResponse uploadBook(MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        // Generate book ID
        UUID bookId = UUID.randomUUID();

        // Process PDF and extract metadata
        Map<String, Object> pdfMetadata = pdfProcessingService.processPdf(file, bookId);

        // Check for duplicate based on file hash
        String fileHash = (String) pdfMetadata.get("fileHash");
        bookRepository.findByFileHash(fileHash).ifPresent(existingBook -> {
            // Clean up the newly created files
            pdfProcessingService.deleteFiles(
                    (String) pdfMetadata.get("pdfPath"),
                    (String) pdfMetadata.get("thumbnailPath")
            );
            throw new DuplicateBookException("A book with the same content already exists: " + existingBook.getTitle());
        });

        // Fetch metadata from Google Books API
        String title = (String) pdfMetadata.getOrDefault("title", originalFilename.replaceFirst("[.][^.]+$", ""));
        String author = (String) pdfMetadata.get("author");

        Map<String, Object> googleMetadata = googleBooksService.fetchMetadata(title, author);

        // Merge metadata (Google Books takes precedence for most fields)
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
        log.info("Book uploaded successfully: {}", book.getTitle());

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

        book = bookRepository.save(book);
        log.info("Book updated successfully: {}", book.getTitle());

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
            if (book.getCurrentPage() != null && book.getPageCount() != null &&
                    book.getCurrentPage() >= book.getPageCount()) {
                book.setStatus(ReadingStatus.FINISHED);
            }
        }

        // Update last read timestamp
        book.setLastReadAt(LocalDateTime.now());

        book = bookRepository.save(book);
        log.info("Progress updated for book: {}", book.getTitle());

        return mapToResponse(book);
    }

    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        // Delete associated notes
        noteService.deleteNotesByBookId(id);

        // Delete files from filesystem
        pdfProcessingService.deleteFiles(book.getPdfPath(), book.getThumbnailPath());

        // Delete cached audio files
        textToSpeechService.deleteBookAudio(id);

        // Delete database record
        bookRepository.delete(book);
        log.info("Book deleted successfully: {}", book.getTitle());
    }

    public LibraryStatsResponse getLibraryStats() {
        long totalBooks = bookRepository.count();
        long unreadBooks = bookRepository.countByStatus(ReadingStatus.UNREAD);
        long readingBooks = bookRepository.countByStatus(ReadingStatus.READING);
        long finishedBooks = bookRepository.countByStatus(ReadingStatus.FINISHED);

        BookResponse continueReading = bookRepository.findMostRecentlyRead()
                .map(this::mapToResponse)
                .orElse(null);

        return LibraryStatsResponse.builder()
                .totalBooks(totalBooks)
                .unreadBooks(unreadBooks)
                .readingBooks(readingBooks)
                .finishedBooks(finishedBooks)
                .continueReading(continueReading)
                .build();
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
