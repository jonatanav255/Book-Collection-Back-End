package com.bookshelf.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Book entity representing a PDF book in the library
 * Stores metadata, reading progress, and file locations
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(length = 500)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String genre;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "current_page")
    private Integer currentPage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingStatus status = ReadingStatus.UNREAD;

    @Column(name = "pdf_path", length = 1000, nullable = false)
    private String pdfPath;

    @Column(name = "thumbnail_path", length = 1000)
    private String thumbnailPath;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "file_hash", length = 64, unique = true)
    private String fileHash;

    @Column(name = "date_added")
    private LocalDateTime dateAdded;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Formula("(COALESCE(current_page, 0) * 1.0 / NULLIF(page_count, 0))")
    private Double progressRatio;

    // No-arg constructor
    public Book() {
    }

    // All-args constructor
    public Book(UUID id, String title, String author, String description, String genre,
                Integer pageCount, Integer currentPage, ReadingStatus status, String pdfPath,
                String thumbnailPath, String coverUrl, String fileHash, LocalDateTime dateAdded,
                LocalDateTime lastReadAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                Double progressRatio) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.genre = genre;
        this.pageCount = pageCount;
        this.currentPage = currentPage;
        this.status = status;
        this.pdfPath = pdfPath;
        this.thumbnailPath = thumbnailPath;
        this.coverUrl = coverUrl;
        this.fileHash = fileHash;
        this.dateAdded = dateAdded;
        this.lastReadAt = lastReadAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.progressRatio = progressRatio;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getProgressRatio() {
        return progressRatio;
    }

    public void setProgressRatio(Double progressRatio) {
        this.progressRatio = progressRatio;
    }

    // Builder

    public static BookBuilder builder() {
        return new BookBuilder();
    }

    public static class BookBuilder {
        private UUID id;
        private String title;
        private String author;
        private String description;
        private String genre;
        private Integer pageCount;
        private Integer currentPage = 0;
        private ReadingStatus status = ReadingStatus.UNREAD;
        private String pdfPath;
        private String thumbnailPath;
        private String coverUrl;
        private String fileHash;
        private LocalDateTime dateAdded;
        private LocalDateTime lastReadAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Double progressRatio;

        BookBuilder() {
        }

        public BookBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public BookBuilder title(String title) {
            this.title = title;
            return this;
        }

        public BookBuilder author(String author) {
            this.author = author;
            return this;
        }

        public BookBuilder description(String description) {
            this.description = description;
            return this;
        }

        public BookBuilder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public BookBuilder pageCount(Integer pageCount) {
            this.pageCount = pageCount;
            return this;
        }

        public BookBuilder currentPage(Integer currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        public BookBuilder status(ReadingStatus status) {
            this.status = status;
            return this;
        }

        public BookBuilder pdfPath(String pdfPath) {
            this.pdfPath = pdfPath;
            return this;
        }

        public BookBuilder thumbnailPath(String thumbnailPath) {
            this.thumbnailPath = thumbnailPath;
            return this;
        }

        public BookBuilder coverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
            return this;
        }

        public BookBuilder fileHash(String fileHash) {
            this.fileHash = fileHash;
            return this;
        }

        public BookBuilder dateAdded(LocalDateTime dateAdded) {
            this.dateAdded = dateAdded;
            return this;
        }

        public BookBuilder lastReadAt(LocalDateTime lastReadAt) {
            this.lastReadAt = lastReadAt;
            return this;
        }

        public BookBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BookBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public BookBuilder progressRatio(Double progressRatio) {
            this.progressRatio = progressRatio;
            return this;
        }

        public Book build() {
            Book book = new Book();
            book.id = this.id;
            book.title = this.title;
            book.author = this.author;
            book.description = this.description;
            book.genre = this.genre;
            book.pageCount = this.pageCount;
            book.currentPage = this.currentPage;
            book.status = this.status;
            book.pdfPath = this.pdfPath;
            book.thumbnailPath = this.thumbnailPath;
            book.coverUrl = this.coverUrl;
            book.fileHash = this.fileHash;
            book.dateAdded = this.dateAdded;
            book.lastReadAt = this.lastReadAt;
            book.createdAt = this.createdAt;
            book.updatedAt = this.updatedAt;
            book.progressRatio = this.progressRatio;
            return book;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDateTime.now();
        }
    }
}
