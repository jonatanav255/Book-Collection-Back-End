package com.bookshelf.service;

import com.bookshelf.dto.PreferencesResponse;
import com.bookshelf.dto.PreferencesUpdateRequest;
import com.bookshelf.model.Preferences;
import com.bookshelf.repository.PreferencesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferencesService {

    private final PreferencesRepository preferencesRepository;

    public PreferencesResponse getPreferences() {
        Preferences preferences = preferencesRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    // Create default preferences if none exist
                    Preferences defaultPrefs = Preferences.builder()
                            .theme("light")
                            .fontFamily("serif")
                            .fontSize("md")
                            .build();
                    return preferencesRepository.save(defaultPrefs);
                });

        return mapToResponse(preferences);
    }

    @Transactional
    public PreferencesResponse updatePreferences(PreferencesUpdateRequest request) {
        Preferences preferences = preferencesRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    Preferences newPrefs = new Preferences();
                    return preferencesRepository.save(newPrefs);
                });

        if (request.getTheme() != null) {
            preferences.setTheme(request.getTheme());
        }
        if (request.getFontFamily() != null) {
            preferences.setFontFamily(request.getFontFamily());
        }
        if (request.getFontSize() != null) {
            preferences.setFontSize(request.getFontSize());
        }

        preferences = preferencesRepository.save(preferences);
        log.info("Preferences updated");

        return mapToResponse(preferences);
    }

    private PreferencesResponse mapToResponse(Preferences preferences) {
        return PreferencesResponse.builder()
                .id(preferences.getId())
                .theme(preferences.getTheme())
                .fontFamily(preferences.getFontFamily())
                .fontSize(preferences.getFontSize())
                .build();
    }
}
