package com.bookshelf.service;

import com.bookshelf.dto.NoteCreateRequest;
import com.bookshelf.dto.NoteResponse;
import com.bookshelf.dto.NoteUpdateRequest;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.Note;
import com.bookshelf.repository.BookRepository;
import com.bookshelf.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private NoteService noteService;

    private UUID bookId;
    private UUID noteId;
    private Book book;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        noteId = UUID.randomUUID();
        book = Book.builder()
                .id(bookId)
                .title("Test Book")
                .author("Test Author")
                .pdfPath("/some/path.pdf")
                .build();
    }

    // ── createNote ────────────────────────────────────────────────────────────

    @Test
    void createNote_savesAndReturnsResponse() {
        NoteCreateRequest request = new NoteCreateRequest(5, "My note", "green", true);
        Note saved = Note.builder()
                .id(noteId)
                .bookId(bookId)
                .pageNumber(5)
                .content("My note")
                .color("green")
                .pinned(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteResponse response = noteService.createNote(bookId, request);

        assertThat(response.getBookId()).isEqualTo(bookId);
        assertThat(response.getPageNumber()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("My note");
        assertThat(response.getColor()).isEqualTo("green");
        assertThat(response.getPinned()).isTrue();
    }

    @Test
    void createNote_usesDefaultColorBlue_whenColorIsNull() {
        NoteCreateRequest request = new NoteCreateRequest(1, "Some content", null, null);
        Note saved = Note.builder()
                .id(noteId).bookId(bookId).pageNumber(1).content("Some content")
                .color("blue").pinned(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            assertThat(n.getColor()).isEqualTo("blue");
            assertThat(n.getPinned()).isFalse();
            return saved;
        });

        noteService.createNote(bookId, request);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void createNote_throwsResourceNotFoundException_whenBookNotFound() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        NoteCreateRequest request = new NoteCreateRequest(1, "content", null, null);
        assertThatThrownBy(() -> noteService.createNote(bookId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(bookId.toString());
    }

    // ── getNotesByBookId ───────────────────────────────────────────────────────

    @Test
    void getNotesByBookId_sortByPage_returnsPinnedFirst() {
        Note unpinned = Note.builder().id(UUID.randomUUID()).bookId(bookId)
                .pageNumber(1).content("Unpinned").color("blue").pinned(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Note pinned = Note.builder().id(UUID.randomUUID()).bookId(bookId)
                .pageNumber(3).content("Pinned").color("red").pinned(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.findByBookIdOrderByPageNumberAsc(bookId))
                .thenReturn(Arrays.asList(unpinned, pinned));

        List<NoteResponse> results = noteService.getNotesByBookId(bookId, "page");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPinned()).isTrue();
        assertThat(results.get(1).getPinned()).isFalse();
    }

    @Test
    void getNotesByBookId_sortByDate_callsDateRepository() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.findByBookIdOrderByCreatedAtDesc(bookId)).thenReturn(List.of());

        noteService.getNotesByBookId(bookId, "date");

        verify(noteRepository).findByBookIdOrderByCreatedAtDesc(bookId);
        verify(noteRepository, never()).findByBookIdOrderByPageNumberAsc(any());
    }

    @Test
    void getNotesByBookId_throwsResourceNotFoundException_whenBookNotFound() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNotesByBookId(bookId, "page"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateNote ────────────────────────────────────────────────────────────

    @Test
    void updateNote_updatesOnlyProvidedFields() {
        Note existing = Note.builder().id(noteId).bookId(bookId).pageNumber(2)
                .content("Old content").color("blue").pinned(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        NoteUpdateRequest request = new NoteUpdateRequest();
        request.setContent("New content");
        // color and pinned left null → should not change

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse response = noteService.updateNote(noteId, request);

        assertThat(response.getContent()).isEqualTo("New content");
        assertThat(response.getColor()).isEqualTo("blue");   // unchanged
        assertThat(response.getPinned()).isFalse();          // unchanged
    }

    @Test
    void updateNote_throwsResourceNotFoundException_whenNoteNotFound() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        NoteUpdateRequest request = new NoteUpdateRequest();
        assertThatThrownBy(() -> noteService.updateNote(noteId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(noteId.toString());
    }

    // ── deleteNote ────────────────────────────────────────────────────────────

    @Test
    void deleteNote_callsRepositoryDelete() {
        Note note = Note.builder().id(noteId).bookId(bookId).pageNumber(1)
                .content("x").color("blue").pinned(false).build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        noteService.deleteNote(noteId);

        verify(noteRepository).delete(note);
    }

    @Test
    void deleteNote_throwsResourceNotFoundException_whenNoteNotFound() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote(noteId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteNotesByBookId ───────────────────────────────────────────────────

    @Test
    void deleteNotesByBookId_deletesAllNotesForBook() {
        noteService.deleteNotesByBookId(bookId);

        verify(noteRepository).deleteByBookId(bookId);
    }

    // ── exportNotesAsMarkdown ─────────────────────────────────────────────────

    @Test
    void exportNotesAsMarkdown_includesBookTitleAndNoteContent() {
        Note note = Note.builder().id(noteId).bookId(bookId).pageNumber(7)
                .content("Important insight").color("yellow").pinned(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.findByBookIdOrderByPageNumberAsc(bookId)).thenReturn(List.of(note));

        String markdown = noteService.exportNotesAsMarkdown(bookId);

        assertThat(markdown).contains("# Notes for: Test Book");
        assertThat(markdown).contains("**Author:** Test Author");
        assertThat(markdown).contains("## Page 7");
        assertThat(markdown).contains("Important insight");
    }

    @Test
    void exportNotesAsMarkdown_showsNoNotesMessage_whenEmpty() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.findByBookIdOrderByPageNumberAsc(bookId)).thenReturn(List.of());

        String markdown = noteService.exportNotesAsMarkdown(bookId);

        assertThat(markdown).contains("*No notes available for this book.*");
    }

    @Test
    void exportNotesAsMarkdown_marksPinnedNotes() {
        Note pinned = Note.builder().id(noteId).bookId(bookId).pageNumber(1)
                .content("Star note").color("blue").pinned(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(noteRepository.findByBookIdOrderByPageNumberAsc(bookId)).thenReturn(List.of(pinned));

        String markdown = noteService.exportNotesAsMarkdown(bookId);

        assertThat(markdown).contains("[Pinned]");
    }
}
