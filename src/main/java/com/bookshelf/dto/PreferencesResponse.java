package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesResponse {
    private UUID id;
    private String theme;
    private String fontFamily;
    private String fontSize;
}
