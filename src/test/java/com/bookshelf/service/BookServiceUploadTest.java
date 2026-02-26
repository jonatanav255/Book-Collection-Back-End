package com.bookshelf.service;

import com.bookshelf.exception.DuplicateBookException;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService upload validation and getBookById.
 * Filesystem and PDF processing are mocked.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceUploadTest {

    @Mock private BookRepository bookRepository;
    @Mock private PdfProcessingService pdfProcessingService;
    @Mock private GoogleBooksService googleBooksService;
    @Mock private NoteService noteService;
    @Mock private TextToSpeechService textToSpeechService;

    @InjectMocks
    private BookService bookService;

    // ── uploadBook — file validation ──────────────────────────────────────────

    @Test
    void uploadBook_throwsIllegalArgumentException_whenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile("file", "test.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> bookService.uploadBook(empty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadBook_throwsIllegalArgumentException_whenFileIsNotPdf() {
        MockMultipartFile notPdf = new MockMultipartFile("file", "book.txt",
                "text/plain", "some text".getBytes());

        assertThatThrownBy(() -> bookService.uploadBook(notPdf))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadBook_throwsDuplicateBookException_whenFileHashAlreadyExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "book.pdf",
                "application/pdf", "%PDF-1.4 test content".getBytes());

        // Simulate PdfProcessingService returning a Map with processed metadata
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("title", "Test Title");
        metadata.put("author", "Author");
        metadata.put("pageCount", 100);
        metadata.put("fileHash", "abc123hash");
        metadata.put("pdfPath", "/storage/temp-id/book.pdf");
        metadata.put("thumbnailPath", "/storage/temp-id/thumbnail.jpg");

        when(pdfProcessingService.processPdf(any(), any())).thenReturn(metadata);

        // Simulate a pre-existing book with the same hash
        Book existing = Book.builder()
                .id(UUID.randomUUID()).title("Test Title").author("Author")
                .pdfPath("/storage/other/book.pdf").status(ReadingStatus.UNREAD)
                .currentPage(0).pageCount(100).fileHash("abc123hash").build();

        when(bookRepository.findByFileHash("abc123hash")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.uploadBook(file))
                .isInstanceOf(DuplicateBookException.class);
    }

    // ── getBookById ───────────────────────────────────────────────────────────

    @Test
    void getBookById_returnsBook_whenFound() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder()
                .id(id).title("Found It").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.READING).currentPage(50).pageCount(200).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        var response = bookService.getBookById(id);

        assertThat(response.getTitle()).isEqualTo("Found It");
        assertThat(response.getId()).isEqualTo(id);
    }

    @Test
    void getBookById_throwsResourceNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── generateDeterministicUUID — pure logic ────────────────────────────────

    @Test
    void generateDeterministicUUID_returnsSameUUID_forSameInputCalledTwice() throws Exception {
        java.lang.reflect.Method method = BookService.class.getDeclaredMethod(
                "generateDeterministicUUID", String.class);
        method.setAccessible(true);

        // Must be a valid 32+ hex char string (SHA-256 format)
        String hash = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6";
        UUID first  = (UUID) method.invoke(bookService, hash);
        UUID second = (UUID) method.invoke(bookService, hash);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void generateDeterministicUUID_returnsDifferentUUID_forDifferentHash() throws Exception {
        java.lang.reflect.Method method = BookService.class.getDeclaredMethod(
                "generateDeterministicUUID", String.class);
        method.setAccessible(true);

        // Two distinct valid hex hashes
        UUID a = (UUID) method.invoke(bookService, "aaaabbbbccccddddeeee1111222233334444");
        UUID b = (UUID) method.invoke(bookService, "1111222233334444aaaabbbbccccddddeeee");

        assertThat(a).isNotEqualTo(b);
    }
}
