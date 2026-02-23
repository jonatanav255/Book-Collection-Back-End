package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

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

    public BookResponse() {
    }

    public BookResponse(UUID id, String title, String author, String description, String genre, Integer pageCount, Integer currentPage, ReadingStatus status, String coverUrl, String fileHash, LocalDateTime dateAdded, LocalDateTime lastReadAt, Double progressPercentage) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.genre = genre;
        this.pageCount = pageCount;
        this.currentPage = currentPage;
        this.status = status;
        this.coverUrl = coverUrl;
        this.fileHash = fileHash;
        this.dateAdded = dateAdded;
        this.lastReadAt = lastReadAt;
        this.progressPercentage = progressPercentage;
    }

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

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookResponse that = (BookResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(author, that.author) &&
                Objects.equals(description, that.description) &&
                Objects.equals(genre, that.genre) &&
                Objects.equals(pageCount, that.pageCount) &&
                Objects.equals(currentPage, that.currentPage) &&
                status == that.status &&
                Objects.equals(coverUrl, that.coverUrl) &&
                Objects.equals(fileHash, that.fileHash) &&
                Objects.equals(dateAdded, that.dateAdded) &&
                Objects.equals(lastReadAt, that.lastReadAt) &&
                Objects.equals(progressPercentage, that.progressPercentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, description, genre, pageCount, currentPage, status, coverUrl, fileHash, dateAdded, lastReadAt, progressPercentage);
    }

    @Override
    public String toString() {
        return "BookResponse(" +
                "id=" + id +
                ", title=" + title +
                ", author=" + author +
                ", description=" + description +
                ", genre=" + genre +
                ", pageCount=" + pageCount +
                ", currentPage=" + currentPage +
                ", status=" + status +
                ", coverUrl=" + coverUrl +
                ", fileHash=" + fileHash +
                ", dateAdded=" + dateAdded +
                ", lastReadAt=" + lastReadAt +
                ", progressPercentage=" + progressPercentage +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder pageCount(Integer pageCount) {
            this.pageCount = pageCount;
            return this;
        }

        public Builder currentPage(Integer currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        public Builder status(ReadingStatus status) {
            this.status = status;
            return this;
        }

        public Builder coverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
            return this;
        }

        public Builder fileHash(String fileHash) {
            this.fileHash = fileHash;
            return this;
        }

        public Builder dateAdded(LocalDateTime dateAdded) {
            this.dateAdded = dateAdded;
            return this;
        }

        public Builder lastReadAt(LocalDateTime lastReadAt) {
            this.lastReadAt = lastReadAt;
            return this;
        }

        public Builder progressPercentage(Double progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }

        public BookResponse build() {
            return new BookResponse(id, title, author, description, genre, pageCount, currentPage, status, coverUrl, fileHash, dateAdded, lastReadAt, progressPercentage);
        }
    }
}
