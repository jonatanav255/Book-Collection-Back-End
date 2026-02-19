package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteUpdateRequest {
    private String content;
    private String color;
    private Boolean pinned;
}
