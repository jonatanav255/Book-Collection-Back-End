package com.bookshelf.controller;

import com.bookshelf.service.TextToSpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class AudioController {

    private final TextToSpeechService textToSpeechService;

    /**
     * Get audio for a specific page of a book
     * Cache-first: Returns cached audio if available, otherwise generates it
     *
     * @param bookId Book UUID
     * @param pageNumber Page number (1-indexed)
     * @return MP3 audio file
     */
    @GetMapping("/{bookId}/pages/{pageNumber}/audio")
    public ResponseEntity<Resource> getPageAudio(
            @PathVariable UUID bookId,
            @PathVariable int pageNumber) {

        log.info("Received request for audio: book={}, page={}", bookId, pageNumber);

        byte[] audioBytes = textToSpeechService.generateOrGetPageAudio(bookId, pageNumber);

        ByteArrayResource resource = new ByteArrayResource(audioBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"book-" + bookId + "-page-" + pageNumber + ".mp3\"")
                .contentType(MediaType.valueOf("audio/mpeg"))
                .contentLength(audioBytes.length)
                .body(resource);
    }

    /**
     * Check if audio is cached for a specific page
     *
     * @param bookId Book UUID
     * @param pageNumber Page number (1-indexed)
     * @return JSON with cached status
     */
    @GetMapping("/{bookId}/pages/{pageNumber}/audio/status")
    public ResponseEntity<?> getAudioStatus(
            @PathVariable UUID bookId,
            @PathVariable int pageNumber) {

        boolean cached = textToSpeechService.isAudioCached(bookId, pageNumber);

        return ResponseEntity.ok()
                .body(new AudioStatusResponse(bookId, pageNumber, cached));
    }

    /**
     * Delete all cached audio for a book
     *
     * @param bookId Book UUID
     */
    @DeleteMapping("/{bookId}/audio")
    public ResponseEntity<?> deleteBookAudio(@PathVariable UUID bookId) {
        log.info("Deleting all audio for book {}", bookId);

        textToSpeechService.deleteBookAudio(bookId);

        return ResponseEntity.ok()
                .body(new MessageResponse("Audio files deleted successfully"));
    }

    // DTOs
    private record AudioStatusResponse(UUID bookId, int pageNumber, boolean cached) {}
    private record MessageResponse(String message) {}
}
