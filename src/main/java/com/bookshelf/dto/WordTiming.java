package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordTiming {
    private String word;
    private double startTime; // in seconds
    private double endTime;   // in seconds
}
