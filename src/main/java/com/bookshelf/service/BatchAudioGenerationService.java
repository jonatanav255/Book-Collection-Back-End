package com.bookshelf.service;

import com.bookshelf.dto.AudioGenerationProgress;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchAudioGenerationService {

    private final TextToSpeechService textToSpeechService;
    private final BookRepository bookRepository;

    // Store generation progress for each book
    private final Map<UUID, AudioGenerationProgress> progressMap = new ConcurrentHashMap<>();

    // Store cancellation flags
    private final Map<UUID, Boolean> cancellationMap = new ConcurrentHashMap<>();

    /**
     * Start batch generation for all pages of a book
     */
    @Async
    public void startBatchGeneration(UUID bookId) {
        log.info("Starting batch audio generation for book {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        int totalPages = book.getPageCount();

        // Initialize progress
        AudioGenerationProgress progress = AudioGenerationProgress.builder()
                .status("RUNNING")
                .currentPage(0)
                .totalPages(totalPages)
                .progressPercentage(0.0)
                .startedAt(System.currentTimeMillis())
                .build();

        progressMap.put(bookId, progress);
        cancellationMap.put(bookId, false);

        try {
            for (int page = 1; page <= totalPages; page++) {
                // Check for cancellation
                if (Boolean.TRUE.equals(cancellationMap.get(bookId))) {
                    log.info("Batch generation cancelled for book {}", bookId);
                    progress.setStatus("CANCELLED");
                    progress.setCompletedAt(System.currentTimeMillis());
                    progressMap.put(bookId, progress);
                    return;
                }

                // Check if already cached
                if (textToSpeechService.isAudioCached(bookId, page)) {
                    log.info("Page {} already cached, skipping", page);
                } else {
                    // Generate audio for this page
                    log.info("Generating audio for page {} of {}", page, totalPages);
                    textToSpeechService.generateOrGetPageAudio(bookId, page);
                }

                // Update progress
                progress.setCurrentPage(page);
                progress.setProgressPercentage((page * 100.0) / totalPages);
                progressMap.put(bookId, progress);
            }

            // Mark as completed
            progress.setStatus("COMPLETED");
            progress.setCompletedAt(System.currentTimeMillis());
            progressMap.put(bookId, progress);

            log.info("Batch audio generation completed for book {}", bookId);

        } catch (Exception e) {
            log.error("Batch generation failed for book {}", bookId, e);
            progress.setStatus("FAILED");
            progress.setErrorMessage(e.getMessage());
            progress.setCompletedAt(System.currentTimeMillis());
            progressMap.put(bookId, progress);
        } finally {
            // Clean up cancellation flag
            cancellationMap.remove(bookId);
        }
    }

    /**
     * Get generation progress for a book
     */
    public AudioGenerationProgress getProgress(UUID bookId) {
        AudioGenerationProgress progress = progressMap.get(bookId);

        if (progress == null) {
            // Check if book exists
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

            // No active generation - return idle status
            return AudioGenerationProgress.builder()
                    .status("IDLE")
                    .currentPage(0)
                    .totalPages(book.getPageCount())
                    .progressPercentage(0.0)
                    .build();
        }

        return progress;
    }

    /**
     * Cancel ongoing batch generation
     */
    public void cancelGeneration(UUID bookId) {
        log.info("Cancelling batch generation for book {}", bookId);
        cancellationMap.put(bookId, true);
    }

    /**
     * Clear progress data for a book (cleanup after completion)
     */
    public void clearProgress(UUID bookId) {
        progressMap.remove(bookId);
        cancellationMap.remove(bookId);
    }
}
