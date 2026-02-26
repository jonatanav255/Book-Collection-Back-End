package com.bookshelf.service;

import com.bookshelf.dto.LibraryStatsResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService.getLibraryStats().
 * Verifies that the service correctly aggregates repository data
 * and builds the LibraryStatsResponse — no DB, no auth required.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceStatsTest {

    @Mock private BookRepository bookRepository;
    @Mock private PdfProcessingService pdfProcessingService;
    @Mock private GoogleBooksService googleBooksService;
    @Mock private NoteService noteService;
    @Mock private TextToSpeechService textToSpeechService;

    @InjectMocks
    private BookService bookService;

    @Test
    void getLibraryStats_returnsCorrectCounts() {
        when(bookRepository.count()).thenReturn(10L);
        when(bookRepository.countByStatus(ReadingStatus.UNREAD)).thenReturn(3L);
        when(bookRepository.countByStatus(ReadingStatus.READING)).thenReturn(4L);
        when(bookRepository.countByStatus(ReadingStatus.FINISHED)).thenReturn(3L);
        when(bookRepository.sumTotalPages()).thenReturn(1500L);
        when(bookRepository.sumTotalPagesRead()).thenReturn(600L);
        when(bookRepository.findMostRecentlyRead()).thenReturn(Optional.empty());

        LibraryStatsResponse stats = bookService.getLibraryStats();

        assertThat(stats.getTotalBooks()).isEqualTo(10L);
        assertThat(stats.getUnreadBooks()).isEqualTo(3L);
        assertThat(stats.getReadingBooks()).isEqualTo(4L);
        assertThat(stats.getFinishedBooks()).isEqualTo(3L);
        assertThat(stats.getTotalPages()).isEqualTo(1500L);
        assertThat(stats.getTotalPagesRead()).isEqualTo(600L);
    }

    @Test
    void getLibraryStats_setContinueReadingToNull_whenNoRecentBook() {
        when(bookRepository.count()).thenReturn(5L);
        when(bookRepository.countByStatus(ReadingStatus.UNREAD)).thenReturn(5L);
        when(bookRepository.countByStatus(ReadingStatus.READING)).thenReturn(0L);
        when(bookRepository.countByStatus(ReadingStatus.FINISHED)).thenReturn(0L);
        when(bookRepository.sumTotalPages()).thenReturn(0L);
        when(bookRepository.sumTotalPagesRead()).thenReturn(0L);
        when(bookRepository.findMostRecentlyRead()).thenReturn(Optional.empty());

        LibraryStatsResponse stats = bookService.getLibraryStats();

        assertThat(stats.getContinueReading()).isNull();
    }

    @Test
    void getLibraryStats_populatesContinueReading_whenRecentBookExists() {
        UUID recentId = UUID.randomUUID();
        Book recentBook = Book.builder()
                .id(recentId)
                .title("Recent Book")
                .author("Author")
                .pdfPath("/path.pdf")
                .status(ReadingStatus.READING)
                .currentPage(30)
                .pageCount(100)
                .build();

        when(bookRepository.count()).thenReturn(1L);
        when(bookRepository.countByStatus(ReadingStatus.UNREAD)).thenReturn(0L);
        when(bookRepository.countByStatus(ReadingStatus.READING)).thenReturn(1L);
        when(bookRepository.countByStatus(ReadingStatus.FINISHED)).thenReturn(0L);
        when(bookRepository.sumTotalPages()).thenReturn(100L);
        when(bookRepository.sumTotalPagesRead()).thenReturn(30L);
        when(bookRepository.findMostRecentlyRead()).thenReturn(Optional.of(recentBook));

        LibraryStatsResponse stats = bookService.getLibraryStats();

        assertThat(stats.getContinueReading()).isNotNull();
        assertThat(stats.getContinueReading().getId()).isEqualTo(recentId);
        assertThat(stats.getContinueReading().getTitle()).isEqualTo("Recent Book");
    }

    @Test
    void getLibraryStats_returnsZeros_whenLibraryIsEmpty() {
        when(bookRepository.count()).thenReturn(0L);
        when(bookRepository.countByStatus(ReadingStatus.UNREAD)).thenReturn(0L);
        when(bookRepository.countByStatus(ReadingStatus.READING)).thenReturn(0L);
        when(bookRepository.countByStatus(ReadingStatus.FINISHED)).thenReturn(0L);
        when(bookRepository.sumTotalPages()).thenReturn(0L);
        when(bookRepository.sumTotalPagesRead()).thenReturn(0L);
        when(bookRepository.findMostRecentlyRead()).thenReturn(Optional.empty());

        LibraryStatsResponse stats = bookService.getLibraryStats();

        assertThat(stats.getTotalBooks()).isZero();
        assertThat(stats.getUnreadBooks()).isZero();
        assertThat(stats.getReadingBooks()).isZero();
        assertThat(stats.getFinishedBooks()).isZero();
        assertThat(stats.getTotalPages()).isZero();
        assertThat(stats.getTotalPagesRead()).isZero();
    }

    @Test
    void getLibraryStats_continueReading_progressPercentage_isCorrect() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder()
                .id(id).title("Half Done").author("A").pdfPath("/p.pdf")
                .status(ReadingStatus.READING).currentPage(50).pageCount(100).build();

        when(bookRepository.count()).thenReturn(1L);
        when(bookRepository.countByStatus(any())).thenReturn(0L);
        when(bookRepository.sumTotalPages()).thenReturn(100L);
        when(bookRepository.sumTotalPagesRead()).thenReturn(50L);
        when(bookRepository.findMostRecentlyRead()).thenReturn(Optional.of(book));

        LibraryStatsResponse stats = bookService.getLibraryStats();

        assertThat(stats.getContinueReading().getProgressPercentage()).isEqualTo(50.0);
    }
}
