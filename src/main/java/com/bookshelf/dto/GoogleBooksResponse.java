package com.bookshelf.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse {
    private List<BookItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookItem {
        private VolumeInfo volumeInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String description;
        private List<String> categories;
        private ImageLinks imageLinks;
        private Integer pageCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinks {
        private String thumbnail;
        private String smallThumbnail;
    }
}
