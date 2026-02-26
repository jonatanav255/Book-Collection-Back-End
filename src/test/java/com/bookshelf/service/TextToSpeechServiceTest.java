package com.bookshelf.service;

import com.bookshelf.dto.WordTiming;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TextToSpeechService.
 *
 * Uses the package-private constructor that accepts a TextToSpeechClient,
 * so TextToSpeechClient.create() (which needs Google credentials) is never called.
 */
@ExtendWith(MockitoExtension.class)
class TextToSpeechServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private TextToSpeechClient ttsClient;

    private TextToSpeechService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new TextToSpeechService(bookRepository, ttsClient);
        setField("audioDirectory", "/tmp/test-audio");
        setField("voiceName",     "en-US-Studio-Q");
        setField("languageCode",  "en-US");
        setField("speakingRate",  1.0d);
        setField("pitch",         0.0d);
    }

    // ── generateOrGetPageAudio — early-exit validation ───────────────────────

    @Test
    void generateOrGetPageAudio_throwsResourceNotFoundException_whenBookNotFound() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateOrGetPageAudio(id, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void generateOrGetPageAudio_throwsIllegalArgumentException_whenPageZero() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.of(book(id, 100)));

        assertThatThrownBy(() -> service.generateOrGetPageAudio(id, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid page number");
    }

    @Test
    void generateOrGetPageAudio_throwsIllegalArgumentException_whenPageExceedsTotal() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.of(book(id, 50)));

        assertThatThrownBy(() -> service.generateOrGetPageAudio(id, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── generatePageAudioWithTimings — early-exit validation ─────────────────

    @Test
    void generatePageAudioWithTimings_throwsResourceNotFoundException_whenBookMissing() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generatePageAudioWithTimings(id, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generatePageAudioWithTimings_throwsIllegalArgument_whenPageTooHigh() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.of(book(id, 10)));

        assertThatThrownBy(() -> service.generatePageAudioWithTimings(id, 11))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── generateEstimatedWordTimings — pure math ──────────────────────────────

    @Test
    void wordTimings_countMatchesWordCount() throws Exception {
        assertThat(timings("hello world test")).hasSize(3);
    }

    @Test
    void wordTimings_preservesWordText() throws Exception {
        assertThat(timings("alpha beta gamma"))
                .extracting(WordTiming::getWord)
                .containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void wordTimings_startTimesAreMonotonicallyIncreasing() throws Exception {
        List<WordTiming> t = timings("one two three four five");
        for (int i = 1; i < t.size(); i++) {
            assertThat(t.get(i).getStartTime()).isGreaterThan(t.get(i - 1).getStartTime());
        }
    }

    @Test
    void wordTimings_endTimeAlwaysAfterStartTime() throws Exception {
        timings("reading is fun and great").forEach(t ->
                assertThat(t.getEndTime()).isGreaterThan(t.getStartTime()));
    }

    @Test
    void wordTimings_durationClampedBetweenMinAndMax() throws Exception {
        timings("a supercalifragilisticexpialidocious").forEach(t -> {
            double duration = t.getEndTime() - t.getStartTime();
            assertThat(duration).isBetween(0.15, 0.81);
        });
    }

    // ── isAudioCached ─────────────────────────────────────────────────────────

    @Test
    void isAudioCached_returnsFalse_whenFileDoesNotExist() {
        assertThat(service.isAudioCached(UUID.randomUUID(), 99)).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<WordTiming> timings(String text) throws Exception {
        Method m = TextToSpeechService.class.getDeclaredMethod("generateEstimatedWordTimings", String.class);
        m.setAccessible(true);
        return (List<WordTiming>) m.invoke(service, text);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = TextToSpeechService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private Book book(UUID id, int pages) {
        return Book.builder().id(id).title("T").author("A").pdfPath("/p.pdf")
                .status(ReadingStatus.UNREAD).currentPage(0).pageCount(pages).build();
    }
}
