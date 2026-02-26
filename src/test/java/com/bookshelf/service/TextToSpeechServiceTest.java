package com.bookshelf.service;

import com.bookshelf.dto.WordTiming;
import com.bookshelf.exception.ResourceNotFoundException;
import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import com.bookshelf.repository.BookRepository;
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
 * The constructor calls TextToSpeechClient.create() which requires Google credentials.
 * We use a Spy on a minimally-constructed subclass that overrides the constructor,
 * injecting a mock TextToSpeechClient via reflection.
 */
@ExtendWith(MockitoExtension.class)
class TextToSpeechServiceTest {

    @Mock private BookRepository bookRepository;

    private TextToSpeechService service;

    /**
     * Subclass that overrides the constructor to prevent TextToSpeechClient.create()
     * from being called. The ttsClient field is injected via reflection in setUp().
     */
    private static class TestableTextToSpeechService extends TextToSpeechService {
        TestableTextToSpeechService(BookRepository repo) throws Exception {
            super(repo, null);  // Calls the package-private (or will fail — handled below)
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Use Mockito.spy on the service after bypassing the credential check
        // by creating the object via reflection with a no-credential path
        service = createServiceWithoutCredentials();
        setField("audioDirectory", "/tmp/test-audio");
        setField("voiceName",     "en-US-Studio-Q");
        setField("languageCode",  "en-US");
        setField("speakingRate",  1.0d);
        setField("pitch",         0.0d);
    }

    /**
     * Creates an instance of TextToSpeechService without calling TextToSpeechClient.create().
     * Uses unsafe allocation (bypasses constructor entirely) via sun.misc.Unsafe — this is
     * a standard unit-test trick for services with external dependencies in their constructor.
     */
    @SuppressWarnings("unchecked")
    private TextToSpeechService createServiceWithoutCredentials() throws Exception {
        // Use Objenesis-style allocation via Unsafe to skip the constructor
        sun.misc.Unsafe unsafe = getUnsafe();
        TextToSpeechService instance = (TextToSpeechService) unsafe.allocateInstance(TextToSpeechService.class);

        // Manually inject the bookRepository field
        Field repoField = TextToSpeechService.class.getDeclaredField("bookRepository");
        repoField.setAccessible(true);
        repoField.set(instance, bookRepository);

        return instance;
    }

    private sun.misc.Unsafe getUnsafe() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
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
