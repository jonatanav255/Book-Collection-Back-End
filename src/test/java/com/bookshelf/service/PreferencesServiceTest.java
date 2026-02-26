package com.bookshelf.service;

import com.bookshelf.dto.PreferencesResponse;
import com.bookshelf.dto.PreferencesUpdateRequest;
import com.bookshelf.model.Preferences;
import com.bookshelf.repository.PreferencesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PreferencesService — singleton preferences record pattern.
 * No DB or Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class PreferencesServiceTest {

    @Mock private PreferencesRepository preferencesRepository;

    @InjectMocks
    private PreferencesService preferencesService;

    // ── getPreferences ────────────────────────────────────────────────────────

    @Test
    void getPreferences_returnsExistingRecord_whenPresent() {
        Preferences existing = Preferences.builder()
                .id(UUID.randomUUID())
                .theme("dark")
                .fontFamily("sans-serif")
                .fontSize("lg")
                .build();

        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        PreferencesResponse response = preferencesService.getPreferences();

        assertThat(response.getTheme()).isEqualTo("dark");
        assertThat(response.getFontFamily()).isEqualTo("sans-serif");
        assertThat(response.getFontSize()).isEqualTo("lg");
    }

    @Test
    void getPreferences_createsDefaultRecord_whenNoneExists() {
        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(Preferences.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PreferencesResponse response = preferencesService.getPreferences();

        assertThat(response.getTheme()).isEqualTo("light");
        assertThat(response.getFontFamily()).isEqualTo("serif");
        assertThat(response.getFontSize()).isEqualTo("md");
    }

    @Test
    void getPreferences_savesDefault_whenNoneExists() {
        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(Preferences.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        preferencesService.getPreferences();

        verify(preferencesRepository).save(any(Preferences.class));
    }

    @Test
    void getPreferences_doesNotSave_whenRecordAlreadyExists() {
        Preferences existing = Preferences.builder()
                .id(UUID.randomUUID()).theme("light").fontFamily("serif").fontSize("md").build();

        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        preferencesService.getPreferences();

        verify(preferencesRepository, never()).save(any());
    }

    // ── updatePreferences ─────────────────────────────────────────────────────

    @Test
    void updatePreferences_updatesTheme_whenProvided() {
        Preferences existing = Preferences.builder()
                .id(UUID.randomUUID()).theme("light").fontFamily("serif").fontSize("md").build();

        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(Preferences.class))).thenAnswer(inv -> inv.getArgument(0));

        PreferencesUpdateRequest request = new PreferencesUpdateRequest();
        request.setTheme("dark");

        PreferencesResponse response = preferencesService.updatePreferences(request);

        assertThat(response.getTheme()).isEqualTo("dark");
    }

    @Test
    void updatePreferences_updatesFontFamily_whenProvided() {
        Preferences existing = Preferences.builder()
                .id(UUID.randomUUID()).theme("light").fontFamily("serif").fontSize("md").build();

        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(Preferences.class))).thenAnswer(inv -> inv.getArgument(0));

        PreferencesUpdateRequest request = new PreferencesUpdateRequest();
        request.setFontFamily("monospace");

        PreferencesResponse response = preferencesService.updatePreferences(request);

        assertThat(response.getFontFamily()).isEqualTo("monospace");
    }

    @Test
    void updatePreferences_keepsPreviousValues_whenFieldsAreNull() {
        Preferences existing = Preferences.builder()
                .id(UUID.randomUUID()).theme("dark").fontFamily("sans").fontSize("xl").build();

        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(Preferences.class))).thenAnswer(inv -> inv.getArgument(0));

        PreferencesUpdateRequest emptyRequest = new PreferencesUpdateRequest();

        PreferencesResponse response = preferencesService.updatePreferences(emptyRequest);

        assertThat(response.getTheme()).isEqualTo("dark");
        assertThat(response.getFontFamily()).isEqualTo("sans");
        assertThat(response.getFontSize()).isEqualTo("xl");
    }

    @Test
    void updatePreferences_savesExactlyOnce() {
        Preferences existing = Preferences.builder()
                .id(UUID.randomUUID()).theme("light").fontFamily("serif").fontSize("md").build();

        when(preferencesRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(Preferences.class))).thenAnswer(inv -> inv.getArgument(0));

        preferencesService.updatePreferences(new PreferencesUpdateRequest());

        verify(preferencesRepository, times(1)).save(any(Preferences.class));
    }
}
