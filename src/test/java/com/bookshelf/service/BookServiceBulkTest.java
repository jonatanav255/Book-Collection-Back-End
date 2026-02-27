package com.bookshelf.service;

import com.bookshelf.dto.BulkOperationResponse;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService bulk operations: deleteBooks() and updateBooksStatus().
 */
@ExtendWith(MockitoExtension.class)
class BookServiceBulkTest {

    @Mock private BookRepository bookRepository;
    @Mock private PdfProcessingService pdfProcessingService;
    @Mock private GoogleBooksService googleBooksService;
    @Mock private NoteService noteService;
    @Mock private TextToSpeechService textToSpeechService;

    @InjectMocks
    private BookService bookService;

    // ── deleteBooks ───────────────────────────────────────────────────────────

    @Test
    void deleteBooks_allFound_deletesAllAndReportsSuccess() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Book book1 = Book.builder().id(id1).title("Book 1").author("A").pdfPath("/f1.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();
        Book book2 = Book.builder().id(id2).title("Book 2").author("B").pdfPath("/f2.pdf")
                .status(ReadingStatus.READING).currentPage(50).pageCount(200).build();

        when(bookRepository.findById(id1)).thenReturn(Optional.of(book1));
        when(bookRepository.findById(id2)).thenReturn(Optional.of(book2));

        BulkOperationResponse response = bookService.deleteBooks(List.of(id1, id2));

        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isZero();
        assertThat(response.getFailedIds()).isEmpty();
        verify(bookRepository).delete(book1);
        verify(bookRepository).delete(book2);
        verify(noteService).deleteNotesByBookId(id1);
        verify(noteService).deleteNotesByBookId(id2);
    }

    @Test
    void deleteBooks_someNotFound_reportsPartialFailure() {
        UUID found = UUID.randomUUID();
        UUID notFound = UUID.randomUUID();
        Book book = Book.builder().id(found).title("Book").author("A").pdfPath("/f.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(found)).thenReturn(Optional.of(book));
        when(bookRepository.findById(notFound)).thenReturn(Optional.empty());

        BulkOperationResponse response = bookService.deleteBooks(List.of(found, notFound));

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(1);
        assertThat(response.getFailedIds()).containsExactly(notFound);
    }

    @Test
    void deleteBooks_allNotFound_reportsAllFailed() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(bookRepository.findById(id1)).thenReturn(Optional.empty());
        when(bookRepository.findById(id2)).thenReturn(Optional.empty());

        BulkOperationResponse response = bookService.deleteBooks(List.of(id1, id2));

        assertThat(response.getSuccessCount()).isZero();
        assertThat(response.getFailureCount()).isEqualTo(2);
        assertThat(response.getFailedIds()).containsExactlyInAnyOrder(id1, id2);
        verify(bookRepository, never()).delete(any(Book.class));
    }

    // ── updateBooksStatus ─────────────────────────────────────────────────────

    @Test
    void updateBooksStatus_allUpdated_reportsSuccess() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(bookRepository.updateStatusByIdIn(List.of(id1, id2), ReadingStatus.FINISHED)).thenReturn(2);

        BulkOperationResponse response = bookService.updateBooksStatus(List.of(id1, id2), ReadingStatus.FINISHED);

        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isZero();
        assertThat(response.getFailedIds()).isEmpty();
    }

    @Test
    void updateBooksStatus_someNotFound_reportsPartialFailure() {
        UUID found = UUID.randomUUID();
        UUID notFound = UUID.randomUUID();

        when(bookRepository.updateStatusByIdIn(List.of(found, notFound), ReadingStatus.READING)).thenReturn(1);
        when(bookRepository.existsById(found)).thenReturn(true);
        when(bookRepository.existsById(notFound)).thenReturn(false);

        BulkOperationResponse response = bookService.updateBooksStatus(List.of(found, notFound), ReadingStatus.READING);

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(1);
        assertThat(response.getFailedIds()).containsExactly(notFound);
    }
}
