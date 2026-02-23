package com.bookshelf.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Note entity representing user annotations on book pages
 * Supports color-coding, pinning, and page-specific notes
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private Boolean pinned = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // No-arg constructor
    public Note() {
    }

    // All-args constructor
    public Note(UUID id, UUID bookId, Integer pageNumber, String content, String color,
                Boolean pinned, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.content = content;
        this.color = color;
        this.pinned = pinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

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

    // Builder

    public static NoteBuilder builder() {
        return new NoteBuilder();
    }

    public static class NoteBuilder {
        private UUID id;
        private UUID bookId;
        private Integer pageNumber;
        private String content;
        private String color;
        private Boolean pinned = false;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        NoteBuilder() {
        }

        public NoteBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public NoteBuilder bookId(UUID bookId) {
            this.bookId = bookId;
            return this;
        }

        public NoteBuilder pageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public NoteBuilder content(String content) {
            this.content = content;
            return this;
        }

        public NoteBuilder color(String color) {
            this.color = color;
            return this;
        }

        public NoteBuilder pinned(Boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        public NoteBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NoteBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Note build() {
            Note note = new Note();
            note.id = this.id;
            note.bookId = this.bookId;
            note.pageNumber = this.pageNumber;
            note.content = this.content;
            note.color = this.color;
            note.pinned = this.pinned;
            note.createdAt = this.createdAt;
            note.updatedAt = this.updatedAt;
            return note;
        }
    }

    /**
     * Set timestamps on creation
     * Both createdAt and updatedAt are set to the same exact timestamp
     * to prevent notes from appearing as "edited" immediately after creation
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        // Set updatedAt to same value as createdAt on creation
        updatedAt = createdAt;
    }

    /**
     * Update timestamp only on actual updates (not creation)
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
