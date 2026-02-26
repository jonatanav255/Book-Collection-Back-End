package com.bookshelf.service;

import com.bookshelf.dto.BookResponse;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService.getAllBooks() filter/sort routing
 * and getBookById() — no DB, no auth, pure business logic.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceGetBooksTest {

    @Mock private BookRepository bookRepository;
    @Mock private PdfProcessingService pdfProcessingService;
    @Mock private GoogleBooksService googleBooksService;
    @Mock private NoteService noteService;
    @Mock private TextToSpeechService textToSpeechService;

    @InjectMocks
    private BookService bookService;

    private Page<Book> emptyPage() {
        return new PageImpl<>(List.of());
    }

    // ── Sort routing ──────────────────────────────────────────────────────────

    @Test
    void getAllBooks_noSearchNoStatus_callsFindAll() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(emptyPage());

        bookService.getAllBooks(null, "dateAdded", null, 0, 20);

        verify(bookRepository).findAll(any(Pageable.class));
        verify(bookRepository, never()).searchBooks(any(), any());
        verify(bookRepository, never()).findByStatus(any(), any());
    }

    @Test
    void getAllBooks_withSearchOnly_callsSearchBooks() {
        when(bookRepository.searchBooks(eq("dune"), any(Pageable.class))).thenReturn(emptyPage());

        bookService.getAllBooks("dune", "title", null, 0, 20);

        verify(bookRepository).searchBooks(eq("dune"), any(Pageable.class));
    }

    @Test
    void getAllBooks_withStatusOnly_callsFindByStatus() {
        when(bookRepository.findByStatus(eq(ReadingStatus.READING), any(Pageable.class)))
                .thenReturn(emptyPage());

        bookService.getAllBooks(null, "dateAdded", "READING", 0, 20);

        verify(bookRepository).findByStatus(eq(ReadingStatus.READING), any(Pageable.class));
    }

    @Test
    void getAllBooks_withSearchAndStatus_callsSearchBooksByStatus() {
        when(bookRepository.searchBooksByStatus(eq("tolkien"), eq(ReadingStatus.FINISHED), any(Pageable.class)))
                .thenReturn(emptyPage());

        bookService.getAllBooks("tolkien", "title", "FINISHED", 0, 20);

        verify(bookRepository).searchBooksByStatus(eq("tolkien"), eq(ReadingStatus.FINISHED), any(Pageable.class));
    }

    @Test
    void getAllBooks_sortByTitle_usesAscTitleSort() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(bookRepository.findAll(pageableCaptor.capture())).thenReturn(emptyPage());

        bookService.getAllBooks(null, "title", null, 0, 20);

        Sort sort = pageableCaptor.getValue().getSort();
        Sort.Order order = sort.getOrderFor("title");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllBooks_sortByLastRead_usesDescLastReadAtSort() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(bookRepository.findAll(pageableCaptor.capture())).thenReturn(emptyPage());

        bookService.getAllBooks(null, "lastRead", null, 0, 20);

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("lastReadAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllBooks_sortByProgress_usesDescProgressRatioSort() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(bookRepository.findAll(pageableCaptor.capture())).thenReturn(emptyPage());

        bookService.getAllBooks(null, "progress", null, 0, 20);

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("progressRatio");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllBooks_unknownSortKey_defaultsToDateAddedDesc() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(bookRepository.findAll(pageableCaptor.capture())).thenReturn(emptyPage());

        bookService.getAllBooks(null, "unknownSort", null, 0, 20);

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("dateAdded");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    // ── getBookById ───────────────────────────────────────────────────────────

    @Test
    void getBookById_returnsResponse_whenBookExists() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder()
                .id(id).title("Dune").author("Herbert").pdfPath("/dune.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(412).build();

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        BookResponse response = bookService.getBookById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getTitle()).isEqualTo("Dune");
    }

    @Test
    void getBookById_throwsResourceNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── whitespace search is ignored ──────────────────────────────────────────

    @Test
    void getAllBooks_blankSearch_treatedAsNoSearch() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(emptyPage());

        bookService.getAllBooks("   ", "dateAdded", null, 0, 20);

        verify(bookRepository).findAll(any(Pageable.class));
        verify(bookRepository, never()).searchBooks(any(), any());
    }
}
