package com.bookshelf.service;

import com.bookshelf.dto.PageTextWithTimings;
import com.bookshelf.dto.WordTiming;
import com.bookshelf.exception.PdfProcessingException;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.repository.BookRepository;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TextToSpeechService {

    private final BookRepository bookRepository;
    private final TextToSpeechClient textToSpeechClient;

    @Value("${bookshelf.storage.audio-directory}")
    private String audioDirectory;

    @Value("${google.cloud.text-to-speech.voice-name}")
    private String voiceName;

    @Value("${google.cloud.text-to-speech.language-code}")
    private String languageCode;

    @Value("${google.cloud.text-to-speech.speaking-rate}")
    private double speakingRate;

    @Value("${google.cloud.text-to-speech.pitch}")
    private double pitch;

    public TextToSpeechService(BookRepository bookRepository) throws IOException {
        this.bookRepository = bookRepository;
        this.textToSpeechClient = TextToSpeechClient.create();
    }

    /**
     * Generate or retrieve cached audio for a specific page of a book
     * Cache-first strategy: Check filesystem cache before calling Google TTS API
     */
    public byte[] generateOrGetPageAudio(UUID bookId, int pageNumber) {
        try {
            // Validate book exists
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

            // Validate page number
            if (pageNumber < 1 || pageNumber > book.getPageCount()) {
                throw new IllegalArgumentException(
                        "Invalid page number: " + pageNumber + ". Book has " + book.getPageCount() + " pages.");
            }

            // Check cache first
            Path audioFile = getAudioFilePath(bookId, pageNumber);
            if (Files.exists(audioFile)) {
                log.info("Serving cached audio for book {} page {}", bookId, pageNumber);
                return Files.readAllBytes(audioFile);
            }

            // Not cached - generate from Google TTS
            log.info("Generating audio for book {} page {} (not in cache)", bookId, pageNumber);

            String pageText = extractPageText(book.getPdfPath(), pageNumber);
            byte[] audioBytes = synthesizeSpeech(pageText);

            // Save to cache for future requests
            Files.createDirectories(audioFile.getParent());
            Files.write(audioFile, audioBytes);
            log.info("Cached audio for book {} page {}", bookId, pageNumber);

            return audioBytes;

        } catch (IOException e) {
            log.error("Failed to generate audio for book {} page {}", bookId, pageNumber, e);
            throw new PdfProcessingException("Failed to generate audio", e);
        }
    }

    /**
     * Get page text for read-along highlighting
     * Returns the text and audio URL - frontend will estimate word positions
     */
    public PageTextWithTimings generatePageAudioWithTimings(UUID bookId, int pageNumber) {
        try {
            // Validate book exists
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

            // Validate page number
            if (pageNumber < 1 || pageNumber > book.getPageCount()) {
                throw new IllegalArgumentException(
                        "Invalid page number: " + pageNumber + ". Book has " + book.getPageCount() + " pages.");
            }

            // Extract page text
            String pageText = extractPageText(book.getPdfPath(), pageNumber);

            // Generate or get cached audio (ensures audio exists)
            generateOrGetPageAudio(bookId, pageNumber);

            // Build audio URL
            String audioUrl = "/api/books/" + bookId + "/pages/" + pageNumber + "/audio";

            // Generate estimated word timings based on text length
            List<WordTiming> wordTimings = generateEstimatedWordTimings(pageText);

            return PageTextWithTimings.builder()
                    .text(pageText)
                    .wordTimings(wordTimings)
                    .audioUrl(audioUrl)
                    .build();

        } catch (IOException e) {
            log.error("Failed to generate page text with timings for book {} page {}", bookId, pageNumber, e);
            throw new PdfProcessingException("Failed to generate page text with timings", e);
        }
    }

    /**
     * Generate estimated word timings based on average speaking rate
     * Assumes ~150 words per minute (2.5 words per second)
     */
    private List<WordTiming> generateEstimatedWordTimings(String text) {
        List<WordTiming> timings = new ArrayList<>();
        String[] words = text.split("\\s+");

        // Estimate: 150 words/minute = 0.4 seconds per word on average
        double secondsPerWord = 0.4;
        double currentTime = 0.0;

        for (String word : words) {
            // Adjust timing based on word length (longer words take more time)
            double wordDuration = secondsPerWord * (1.0 + (word.length() - 5) * 0.02);
            wordDuration = Math.max(0.2, Math.min(wordDuration, 1.0)); // Clamp between 0.2-1.0s

            WordTiming timing = WordTiming.builder()
                    .word(word)
                    .startTime(currentTime)
                    .endTime(currentTime + wordDuration)
                    .build();

            timings.add(timing);
            currentTime += wordDuration;
        }

        log.info("Generated {} estimated word timings for {} words", timings.size(), words.length);
        return timings;
    }

    /**
     * Extract text from a specific page of a PDF
     */
    private String extractPageText(String pdfPath, int pageNumber) throws IOException {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new PdfProcessingException("PDF file not found at path: " + pdfPath);
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);

            String text = stripper.getText(document);

            // Clean up the text
            text = text.trim();

            if (text.isEmpty()) {
                log.warn("Page {} is empty or contains no extractable text", pageNumber);
                return "This page appears to be empty or contains only images.";
            }

            // Google TTS has a 5000 character limit per request
            if (text.length() > 5000) {
                log.warn("Page {} text exceeds 5000 chars ({}), truncating", pageNumber, text.length());
                text = text.substring(0, 5000);
            }

            log.info("Extracted {} characters from page {}", text.length(), pageNumber);
            return text;

        } catch (IOException e) {
            log.error("Failed to extract text from page {}", pageNumber, e);
            throw new PdfProcessingException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Call Google Cloud Text-to-Speech API to synthesize speech
     */
    private byte[] synthesizeSpeech(String text) throws IOException {
        try {
            // Build the synthesis input
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(text)
                    .build();

            // Build the voice selection
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode)
                    .setName(voiceName)
                    .setSsmlGender(SsmlVoiceGender.MALE) // Studio-Q is male
                    .build();

            // Build the audio config
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .setSpeakingRate(speakingRate)
                    .setPitch(pitch)
                    .build();

            // Perform the text-to-speech request
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
                    input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();
            byte[] audioBytes = audioContents.toByteArray();

            log.info("Generated {} bytes of audio from {} characters of text",
                    audioBytes.length, text.length());

            return audioBytes;

        } catch (Exception e) {
            log.error("Failed to synthesize speech with Google TTS", e);
            throw new IOException("Failed to call Google Text-to-Speech API", e);
        }
    }

    /**
     * Get the file path for cached audio
     */
    private Path getAudioFilePath(UUID bookId, int pageNumber) {
        Path audioDir = Paths.get(audioDirectory, bookId.toString()).toAbsolutePath().normalize();
        return audioDir.resolve("page-" + pageNumber + ".mp3");
    }

    /**
     * Delete all cached audio files for a book
     */
    public void deleteBookAudio(UUID bookId) {
        try {
            Path bookAudioDir = Paths.get(audioDirectory, bookId.toString()).toAbsolutePath();
            if (Files.exists(bookAudioDir)) {
                Files.walk(bookAudioDir)
                        .sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete audio file: {}", path, e);
                            }
                        });
                log.info("Deleted all audio files for book {}", bookId);
            }
        } catch (IOException e) {
            log.error("Failed to delete audio directory for book {}", bookId, e);
        }
    }

    /**
     * Check if audio exists in cache
     */
    public boolean isAudioCached(UUID bookId, int pageNumber) {
        Path audioFile = getAudioFilePath(bookId, pageNumber);
        return Files.exists(audioFile);
    }
}
