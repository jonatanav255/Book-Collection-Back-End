package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesUpdateRequest {
    private String theme;
    private String fontFamily;
    private String fontSize;
}
