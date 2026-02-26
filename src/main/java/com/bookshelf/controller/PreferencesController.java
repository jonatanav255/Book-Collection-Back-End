package com.bookshelf.controller;

import com.bookshelf.dto.PreferencesResponse;
import com.bookshelf.dto.PreferencesUpdateRequest;
import com.bookshelf.service.PreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Preferences", description = "User preferences — theme, font size, font family")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @Operation(summary = "Get current preferences")
    @GetMapping
    public ResponseEntity<PreferencesResponse> getPreferences() {
        PreferencesResponse response = preferencesService.getPreferences();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update preferences")
    @PutMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @Valid @RequestBody PreferencesUpdateRequest request) {
        PreferencesResponse response = preferencesService.updatePreferences(request);
        return ResponseEntity.ok(response);
    }
}
