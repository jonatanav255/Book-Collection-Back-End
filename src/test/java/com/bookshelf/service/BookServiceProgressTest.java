package com.bookshelf.service;

import com.bookshelf.dto.BookResponse;
import com.bookshelf.dto.ProgressUpdateRequest;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService.updateProgress() auto-status transition logic.
 * No database, no Spring context, no auth needed.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceProgressTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private PdfProcessingService pdfProcessingService;
    @Mock
    private GoogleBooksService googleBooksService;
    @Mock
    private NoteService noteService;
    @Mock
    private TextToSpeechService textToSpeechService;

    @InjectMocks
    private BookService bookService;

    private UUID bookId;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
    }

    private Book buildBook(ReadingStatus status, int currentPage, int pageCount) {
        return Book.builder()
                .id(bookId)
                .title("Test Book")
                .author("Author")
                .pdfPath("/path/book.pdf")
                .status(status)
                .currentPage(currentPage)
                .pageCount(pageCount)
                .build();
    }

    private void mockSave() {
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Auto-status: UNREAD → READING ─────────────────────────────────────────

    @Test
    void updateProgress_setsStatusToReading_whenUnreadBookHasPageGreaterThanZero() {
        Book book = buildBook(ReadingStatus.UNREAD, 0, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        ProgressUpdateRequest req = new ProgressUpdateRequest(5, null);
        BookResponse response = bookService.updateProgress(bookId, req);

        assertThat(response.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(response.getCurrentPage()).isEqualTo(5);
    }

    @Test
    void updateProgress_keepsStatusUnread_whenCurrentPageIsZero() {
        Book book = buildBook(ReadingStatus.UNREAD, 0, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        ProgressUpdateRequest req = new ProgressUpdateRequest(0, null);
        BookResponse response = bookService.updateProgress(bookId, req);

        assertThat(response.getStatus()).isEqualTo(ReadingStatus.UNREAD);
    }

    // ── Auto-status: READING → FINISHED ───────────────────────────────────────

    @Test
    void updateProgress_setsStatusToFinished_whenCurrentPageReachesPageCount() {
        Book book = buildBook(ReadingStatus.READING, 50, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        ProgressUpdateRequest req = new ProgressUpdateRequest(100, null);
        BookResponse response = bookService.updateProgress(bookId, req);

        assertThat(response.getStatus()).isEqualTo(ReadingStatus.FINISHED);
    }

    @Test
    void updateProgress_setsStatusToFinished_whenCurrentPageExceedsPageCount() {
        Book book = buildBook(ReadingStatus.READING, 95, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        ProgressUpdateRequest req = new ProgressUpdateRequest(101, null);
        BookResponse response = bookService.updateProgress(bookId, req);

        assertThat(response.getStatus()).isEqualTo(ReadingStatus.FINISHED);
    }

    // ── Auto-status: FINISHED → READING (when going back) ────────────────────

    @Test
    void updateProgress_setsStatusBackToReading_whenFinishedBookPageDecreasesBeforeEnd() {
        Book book = buildBook(ReadingStatus.FINISHED, 100, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        ProgressUpdateRequest req = new ProgressUpdateRequest(80, null);
        BookResponse response = bookService.updateProgress(bookId, req);

        assertThat(response.getStatus()).isEqualTo(ReadingStatus.READING);
    }

    // ── Manual status override ─────────────────────────────────────────────────

    @Test
    void updateProgress_respectsExplicitStatusOverride() {
        Book book = buildBook(ReadingStatus.UNREAD, 0, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        ProgressUpdateRequest req = new ProgressUpdateRequest(0, ReadingStatus.FINISHED);
        BookResponse response = bookService.updateProgress(bookId, req);

        // Explicit status wins over auto-detection
        assertThat(response.getStatus()).isEqualTo(ReadingStatus.FINISHED);
    }

    // ── Updates lastReadAt ─────────────────────────────────────────────────────

    @Test
    void updateProgress_updatesLastReadAt() {
        Book book = buildBook(ReadingStatus.READING, 10, 100);
        assertThat(book.getLastReadAt()).isNull();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        bookService.updateProgress(bookId, new ProgressUpdateRequest(20, null));

        assertThat(book.getLastReadAt()).isNotNull();
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void updateProgress_throwsResourceNotFoundException_whenBookNotFound() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateProgress(bookId, new ProgressUpdateRequest(1, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(bookId.toString());
    }

    // ── progressPercentage in response ────────────────────────────────────────

    @Test
    void updateProgress_calculatesProgressPercentageCorrectly() {
        Book book = buildBook(ReadingStatus.READING, 0, 200);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        BookResponse response = bookService.updateProgress(bookId, new ProgressUpdateRequest(50, null));

        assertThat(response.getProgressPercentage()).isEqualTo(25.0);
    }

    // ── currentPage clamping ────────────────────────────────────────────────

    @Test
    void updateProgress_clampsCurrentPageToPageCount_whenExceedsTotal() {
        Book book = buildBook(ReadingStatus.READING, 50, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        BookResponse response = bookService.updateProgress(bookId, new ProgressUpdateRequest(999, null));

        assertThat(response.getCurrentPage()).isEqualTo(100);
        assertThat(response.getStatus()).isEqualTo(ReadingStatus.FINISHED);
        assertThat(response.getProgressPercentage()).isEqualTo(100.0);
    }

    @Test
    void updateProgress_clampsNegativeCurrentPageToZero() {
        Book book = buildBook(ReadingStatus.READING, 50, 100);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        mockSave();

        BookResponse response = bookService.updateProgress(bookId, new ProgressUpdateRequest(-5, null));

        assertThat(response.getCurrentPage()).isEqualTo(0);
    }
}
