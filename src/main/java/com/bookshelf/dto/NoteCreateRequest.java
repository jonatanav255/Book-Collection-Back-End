package com.bookshelf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteCreateRequest {
    @NotNull(message = "Page number is required")
    private Integer pageNumber;

    @NotBlank(message = "Content is required")
    private String content;

    private String color;
    private Boolean pinned = false;
}
