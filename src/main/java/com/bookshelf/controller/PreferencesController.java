package com.bookshelf.controller;

import com.bookshelf.dto.PreferencesResponse;
import com.bookshelf.dto.PreferencesUpdateRequest;
import com.bookshelf.service.PreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public ResponseEntity<PreferencesResponse> getPreferences() {
        PreferencesResponse response = preferencesService.getPreferences();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @RequestBody PreferencesUpdateRequest request) {
        PreferencesResponse response = preferencesService.updatePreferences(request);
        return ResponseEntity.ok(response);
    }
}
