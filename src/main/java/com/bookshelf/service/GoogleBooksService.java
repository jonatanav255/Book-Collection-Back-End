package com.bookshelf.service;

import com.bookshelf.dto.GoogleBooksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with Google Books API
 * Enriches book metadata with cover images, descriptions, and genre information
 */
@Service
public class GoogleBooksService {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksService.class);

    private final WebClient webClient;

    @Value("${google.books.api.key:}")
    private String apiKey;

    public GoogleBooksService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://www.googleapis.com/books/v1")
                .build();
    }

    /**
     * Fetch enriched metadata from Google Books API
     * Searches by title and author, returns first match
     * Gracefully degrades if API fails or returns no results
     *
     * @param title Book title for search query
     * @param author Book author for search query (optional)
     * @return Map containing: title, author, description, genre, coverUrl, pageCount
     */
    public Map<String, Object> fetchMetadata(String title, String author) {
        Map<String, Object> enrichedMetadata = new HashMap<>();

        try {
            // Build search query
            StringBuilder query = new StringBuilder();
            if (title != null && !title.trim().isEmpty()) {
                query.append("intitle:").append(title.trim());
            }
            if (author != null && !author.trim().isEmpty()) {
                if (query.length() > 0) {
                    query.append("+");
                }
                query.append("inauthor:").append(author.trim());
            }

            if (query.length() == 0) {
                return enrichedMetadata;
            }

            // Make API request
            String uri = "/volumes?q=" + query.toString() + "&maxResults=1";
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                uri += "&key=" + apiKey;
            }

            GoogleBooksResponse response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(GoogleBooksResponse.class)
                    .onErrorResume(e -> {
                        log.error("Google Books API error: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                GoogleBooksResponse.VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();

                if (volumeInfo != null) {
                    // Extract title
                    if (volumeInfo.getTitle() != null) {
                        enrichedMetadata.put("title", volumeInfo.getTitle());
                    }

                    // Extract author
                    if (volumeInfo.getAuthors() != null && !volumeInfo.getAuthors().isEmpty()) {
                        enrichedMetadata.put("author", String.join(", ", volumeInfo.getAuthors()));
                    }

                    // Extract description
                    if (volumeInfo.getDescription() != null) {
                        enrichedMetadata.put("description", volumeInfo.getDescription());
                    }

                    // Extract genre/category
                    if (volumeInfo.getCategories() != null && !volumeInfo.getCategories().isEmpty()) {
                        enrichedMetadata.put("genre", volumeInfo.getCategories().get(0));
                    }

                    // Extract cover image URL
                    if (volumeInfo.getImageLinks() != null) {
                        String coverUrl = volumeInfo.getImageLinks().getThumbnail();
                        if (coverUrl == null) {
                            coverUrl = volumeInfo.getImageLinks().getSmallThumbnail();
                        }
                        if (coverUrl != null) {
                            // Upgrade to HTTPS and request higher resolution image
                            coverUrl = coverUrl.replace("http://", "https://");
                            coverUrl = coverUrl.replace("zoom=1", "zoom=2");
                            coverUrl = coverUrl.replace("&edge=curl", "");
                            enrichedMetadata.put("coverUrl", coverUrl);
                        }
                    }

                    // Extract page count (if not already set)
                    if (volumeInfo.getPageCount() != null) {
                        enrichedMetadata.put("pageCount", volumeInfo.getPageCount());
                    }

                }
            } else {
            }

        } catch (Exception e) {
            log.error("Failed to fetch Google Books metadata", e);
            // Graceful degradation - return empty metadata
        }

        return enrichedMetadata;
    }

    /**
     * Search Google Books by general query string
     * Used by frontend book lookup feature
     *
     * @param query Search query string
     * @return GoogleBooksResponse with matching volumes
     */
    @Cacheable(value = "googleBooksSearch", key = "#query")
    public GoogleBooksResponse searchBooks(String query) {
        log.info("CACHE MISS — googleBooksSearch: calling Google API (query={})", query);
        try {
            String uri = "/volumes?q=" + query;
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                uri += "&key=" + apiKey;
            }

            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(GoogleBooksResponse.class)
                    .block();

        } catch (Exception e) {
            log.error("Failed to search Google Books", e);
            return new GoogleBooksResponse();
        }
    }
}
