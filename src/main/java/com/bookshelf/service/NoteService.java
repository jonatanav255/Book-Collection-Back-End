package com.bookshelf.service;

import com.bookshelf.dto.NoteCreateRequest;
import com.bookshelf.dto.NoteResponse;
import com.bookshelf.dto.NoteUpdateRequest;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.Note;
import com.bookshelf.repository.BookRepository;
import com.bookshelf.repository.NoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final BookRepository bookRepository;

    @Transactional
    public NoteResponse createNote(UUID bookId, NoteCreateRequest request) {
        // Verify book exists
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        Note note = Note.builder()
                .bookId(bookId)
                .pageNumber(request.getPageNumber())
                .content(request.getContent())
                .color(request.getColor() != null ? request.getColor() : "blue")
                .pinned(request.getPinned() != null ? request.getPinned() : false)
                .build();

        note = noteRepository.save(note);
        log.info("Note created for book: {}", book.getTitle());

        return mapToResponse(note);
    }

    public List<NoteResponse> getNotesByBookId(UUID bookId, String sortBy) {
        // Verify book exists
        bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        List<Note> notes;
        if ("date".equals(sortBy)) {
            notes = noteRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        } else {
            notes = noteRepository.findByBookIdOrderByPageNumberAsc(bookId);
        }

        // Sort pinned notes to the top
        return notes.stream()
                .sorted(Comparator.comparing(Note::getPinned).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoteResponse updateNote(UUID noteId, NoteUpdateRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + noteId));

        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getColor() != null) {
            note.setColor(request.getColor());
        }
        if (request.getPinned() != null) {
            note.setPinned(request.getPinned());
        }

        note = noteRepository.save(note);
        log.info("Note updated: {}", noteId);

        return mapToResponse(note);
    }

    @Transactional
    public void deleteNote(UUID noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + noteId));

        noteRepository.delete(note);
        log.info("Note deleted: {}", noteId);
    }

    @Transactional
    public void deleteNotesByBookId(UUID bookId) {
        noteRepository.deleteByBookId(bookId);
        log.info("All notes deleted for book: {}", bookId);
    }

    public String exportNotesAsMarkdown(UUID bookId) {
        // Verify book exists and get its details
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        List<Note> notes = noteRepository.findByBookIdOrderByPageNumberAsc(bookId);

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Notes for: ").append(book.getTitle()).append("\n\n");

        if (book.getAuthor() != null) {
            markdown.append("**Author:** ").append(book.getAuthor()).append("\n\n");
        }

        markdown.append("**Exported:** ").append(java.time.LocalDateTime.now()).append("\n\n");
        markdown.append("---\n\n");

        if (notes.isEmpty()) {
            markdown.append("*No notes available for this book.*\n");
        } else {
            for (Note note : notes) {
                markdown.append("## Page ").append(note.getPageNumber()).append("\n\n");

                if (note.getColor() != null) {
                    markdown.append("**Category:** ").append(note.getColor()).append("\n\n");
                }

                markdown.append(note.getContent()).append("\n\n");

                if (note.getPinned()) {
                    markdown.append("*[Pinned]*\n\n");
                }

                markdown.append("*Created: ").append(note.getCreatedAt()).append("*\n\n");
                markdown.append("---\n\n");
            }
        }

        log.info("Exported {} notes for book: {}", notes.size(), book.getTitle());
        return markdown.toString();
    }

    private NoteResponse mapToResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .bookId(note.getBookId())
                .pageNumber(note.getPageNumber())
                .content(note.getContent())
                .color(note.getColor())
                .pinned(note.getPinned())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
