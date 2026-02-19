package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private UUID id;
    private String title;
    private String author;
    private String description;
    private String genre;
    private Integer pageCount;
    private Integer currentPage;
    private ReadingStatus status;
    private String coverUrl;
    private String fileHash;
    private LocalDateTime dateAdded;
    private LocalDateTime lastReadAt;
    private Double progressPercentage;
}
