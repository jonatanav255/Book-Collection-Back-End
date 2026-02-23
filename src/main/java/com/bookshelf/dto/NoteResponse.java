package com.bookshelf.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class NoteResponse {
    private UUID id;
    private UUID bookId;
    private Integer pageNumber;
    private String content;
    private String color;
    private Boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NoteResponse() {
    }

    public NoteResponse(UUID id, UUID bookId, Integer pageNumber, String content, String color, Boolean pinned, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.content = content;
        this.color = color;
        this.pinned = pinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookId() {
        return bookId;
    }

    public void setBookId(UUID bookId) {
        this.bookId = bookId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteResponse that = (NoteResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(bookId, that.bookId) &&
                Objects.equals(pageNumber, that.pageNumber) &&
                Objects.equals(content, that.content) &&
                Objects.equals(color, that.color) &&
                Objects.equals(pinned, that.pinned) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookId, pageNumber, content, color, pinned, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "NoteResponse(" +
                "id=" + id +
                ", bookId=" + bookId +
                ", pageNumber=" + pageNumber +
                ", content=" + content +
                ", color=" + color +
                ", pinned=" + pinned +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID bookId;
        private Integer pageNumber;
        private String content;
        private String color;
        private Boolean pinned;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder bookId(UUID bookId) {
            this.bookId = bookId;
            return this;
        }

        public Builder pageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder pinned(Boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public NoteResponse build() {
            return new NoteResponse(id, bookId, pageNumber, content, color, pinned, createdAt, updatedAt);
        }
    }
}
