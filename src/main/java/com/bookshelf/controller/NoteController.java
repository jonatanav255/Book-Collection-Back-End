package com.bookshelf.controller;

import com.bookshelf.dto.NoteCreateRequest;
import com.bookshelf.dto.NoteResponse;
import com.bookshelf.dto.NoteUpdateRequest;
import com.bookshelf.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Notes", description = "Manage reading notes and annotations per book")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(summary = "Get notes for a book")
    @GetMapping("/books/{bookId}/notes")
    public ResponseEntity<List<NoteResponse>> getNotesByBookId(
            @PathVariable UUID bookId,
            @RequestParam(required = false, defaultValue = "page") String sortBy) {
        List<NoteResponse> notes = noteService.getNotesByBookId(bookId, sortBy);
        return ResponseEntity.ok(notes);
    }

    @Operation(summary = "Create a note")
    @PostMapping("/books/{bookId}/notes")
    public ResponseEntity<NoteResponse> createNote(
            @PathVariable UUID bookId,
            @Valid @RequestBody NoteCreateRequest request) {
        NoteResponse response = noteService.createNote(bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a note")
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteUpdateRequest request) {
        NoteResponse response = noteService.updateNote(noteId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a note")
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Export notes as Markdown")
    @GetMapping("/books/{bookId}/notes/export")
    public ResponseEntity<String> exportNotes(@PathVariable UUID bookId) {
        String markdown = noteService.exportNotesAsMarkdown(bookId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notes.md\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(markdown);
    }
}
