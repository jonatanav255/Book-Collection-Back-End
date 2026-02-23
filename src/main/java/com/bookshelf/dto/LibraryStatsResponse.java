package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryStatsResponse {
    private long totalBooks;
    private long unreadBooks;
    private long readingBooks;
    private long finishedBooks;
    private long totalPages;
    private long totalPagesRead;
    private BookResponse continueReading;
}
