package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerationProgress {
    private String status; // RUNNING, COMPLETED, FAILED, CANCELLED
    private int currentPage;
    private int totalPages;
    private double progressPercentage;
    private String errorMessage;
    private long startedAt;
    private long completedAt;
}
