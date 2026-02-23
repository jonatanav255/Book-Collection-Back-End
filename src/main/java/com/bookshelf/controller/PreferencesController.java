package com.bookshelf.controller;

import com.bookshelf.dto.PreferencesResponse;
import com.bookshelf.dto.PreferencesUpdateRequest;
import com.bookshelf.service.PreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

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
