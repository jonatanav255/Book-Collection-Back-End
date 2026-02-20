package com.bookshelf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageTextWithTimings {
    private String text;
    private List<WordTiming> wordTimings;
    private String audioUrl;
}
