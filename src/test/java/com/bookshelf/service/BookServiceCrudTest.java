package com.bookshelf.service;

import com.bookshelf.dto.BookResponse;
import com.bookshelf.dto.BookUpdateRequest;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
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
 * Unit tests for BookService.updateBook() and deleteBook().
 * No DB, no Spring context — pure business logic with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceCrudTest {

    @Mock private BookRepository bookRepository;
    @Mock private PdfProcessingService pdfProcessingService;
    @Mock private GoogleBooksService googleBooksService;
    @Mock private NoteService noteService;
    @Mock private TextToSpeechService textToSpeechService;

    @InjectMocks
    private BookService bookService;

    // ── updateBook ────────────────────────────────────────────────────────────

    @Test
    void updateBook_throwsResourceNotFoundException_whenBookNotFound() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(id, new BookUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateBook_updatesTitle_whenTitleProvided() {
        UUID id = UUID.randomUUID();
        Book existing = Book.builder()
                .id(id).title("Old Title").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookUpdateRequest request = new BookUpdateRequest();
        request.setTitle("New Title");

        BookResponse response = bookService.updateBook(id, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateBook_updatesAuthor_whenAuthorProvided() {
        UUID id = UUID.randomUUID();
        Book existing = Book.builder()
                .id(id).title("Title").author("Old Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookUpdateRequest request = new BookUpdateRequest();
        request.setAuthor("New Author");

        BookResponse response = bookService.updateBook(id, request);

        assertThat(response.getAuthor()).isEqualTo("New Author");
    }

    @Test
    void updateBook_keepsExistingFields_whenRequestFieldsAreNull() {
        UUID id = UUID.randomUUID();
        Book existing = Book.builder()
                .id(id).title("Keep Me").author("Keep Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookUpdateRequest emptyRequest = new BookUpdateRequest(); // all null fields

        BookResponse response = bookService.updateBook(id, emptyRequest);

        assertThat(response.getTitle()).isEqualTo("Keep Me");
        assertThat(response.getAuthor()).isEqualTo("Keep Author");
    }

    @Test
    void updateBook_updatesStatus_whenStatusProvided() {
        UUID id = UUID.randomUUID();
        Book existing = Book.builder()
                .id(id).title("Title").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookUpdateRequest request = new BookUpdateRequest();
        request.setStatus(ReadingStatus.READING);

        BookResponse response = bookService.updateBook(id, request);

        assertThat(response.getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    void updateBook_savesEntityExactlyOnce() {
        UUID id = UUID.randomUUID();
        Book existing = Book.builder()
                .id(id).title("Title").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.updateBook(id, new BookUpdateRequest());

        verify(bookRepository, times(1)).save(any(Book.class));
    }

    // ── deleteBook ────────────────────────────────────────────────────────────

    @Test
    void deleteBook_throwsResourceNotFoundException_whenBookNotFound() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void deleteBook_deletesAssociatedNotes() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder()
                .id(id).title("Title").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        bookService.deleteBook(id);

        verify(noteService).deleteNotesByBookId(id);
    }

    @Test
    void deleteBook_deletesPdfFiles() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder()
                .id(id).title("Title").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        bookService.deleteBook(id);

        verify(pdfProcessingService).deleteFiles(book.getPdfPath(), book.getThumbnailPath());
    }

    @Test
    void deleteBook_deletesFromRepository() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder()
                .id(id).title("Title").author("Author").pdfPath("/file.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(100).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        bookService.deleteBook(id);

        verify(bookRepository).delete(book);
    }
}
