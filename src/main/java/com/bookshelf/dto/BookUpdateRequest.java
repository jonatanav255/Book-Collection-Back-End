package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdateRequest {
    private String title;
    private String author;
    private String description;
    private String genre;
    private ReadingStatus status;
    private String coverUrl;
    private Integer currentPage;
}
