package com.bookshelf.controller;

import com.bookshelf.dto.AudioGenerationProgress;
import com.bookshelf.dto.PageTextWithTimings;
import com.bookshelf.service.BatchAudioGenerationService;
import com.bookshelf.service.TextToSpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    private final TextToSpeechService textToSpeechService;
    private final BatchAudioGenerationService batchAudioGenerationService;

    public AudioController(TextToSpeechService textToSpeechService,
                           BatchAudioGenerationService batchAudioGenerationService) {
        this.textToSpeechService = textToSpeechService;
        this.batchAudioGenerationService = batchAudioGenerationService;
    }

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
     * Get page text with word-level timestamps for read-along highlighting
     * This generates fresh audio with timing data (not cached)
     *
     * @param bookId Book UUID
     * @param pageNumber Page number (1-indexed)
     * @return JSON with text, word timings, and audio URL
     */
    @GetMapping("/{bookId}/pages/{pageNumber}/text-with-timings")
    public ResponseEntity<PageTextWithTimings> getPageTextWithTimings(
            @PathVariable UUID bookId,
            @PathVariable int pageNumber) {

        PageTextWithTimings result = textToSpeechService.generatePageAudioWithTimings(bookId, pageNumber);

        return ResponseEntity.ok(result);
    }

    /**
     * Delete all cached audio for a book
     *
     * @param bookId Book UUID
     */
    @DeleteMapping("/{bookId}/audio")
    public ResponseEntity<?> deleteBookAudio(@PathVariable UUID bookId) {
        textToSpeechService.deleteBookAudio(bookId);

        return ResponseEntity.ok()
                .body(new MessageResponse("Audio files deleted successfully"));
    }

    /**
     * Start batch generation for pages of a book
     * Optional query parameters: startPage, endPage
     * If not provided, generates all pages (1 to totalPages)
     *
     * @param bookId Book UUID
     * @param startPage Starting page number (optional, defaults to 1)
     * @param endPage Ending page number (optional, defaults to total pages)
     */
    @PostMapping("/{bookId}/audio/generate-all")
    public ResponseEntity<?> startBatchGeneration(
            @PathVariable UUID bookId,
            @RequestParam(required = false) Integer startPage,
            @RequestParam(required = false) Integer endPage) {

        batchAudioGenerationService.startBatchGeneration(bookId, startPage, endPage);

        return ResponseEntity.accepted()
                .body(new MessageResponse("Batch audio generation started"));
    }

    /**
     * Get batch generation progress for a book
     *
     * @param bookId Book UUID
     */
    @GetMapping("/{bookId}/audio/generation-status")
    public ResponseEntity<AudioGenerationProgress> getBatchGenerationStatus(@PathVariable UUID bookId) {
        AudioGenerationProgress progress = batchAudioGenerationService.getProgress(bookId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Cancel ongoing batch generation
     *
     * @param bookId Book UUID
     */
    @DeleteMapping("/{bookId}/audio/generation")
    public ResponseEntity<?> cancelBatchGeneration(@PathVariable UUID bookId) {
        batchAudioGenerationService.cancelGeneration(bookId);

        return ResponseEntity.ok()
                .body(new MessageResponse("Batch generation cancellation requested"));
    }

    // DTOs
    private record AudioStatusResponse(UUID bookId, int pageNumber, boolean cached) {}
    private record MessageResponse(String message) {}
}
