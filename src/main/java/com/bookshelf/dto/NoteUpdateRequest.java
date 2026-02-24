package com.bookshelf.dto;

import jakarta.validation.constraints.Size;

import java.util.Objects;

public class NoteUpdateRequest {
    @Size(max = 10000)
    private String content;
    private String color;
    private Boolean pinned;

    public NoteUpdateRequest() {
    }

    public NoteUpdateRequest(String content, String color, Boolean pinned) {
        this.content = content;
        this.color = color;
        this.pinned = pinned;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteUpdateRequest that = (NoteUpdateRequest) o;
        return Objects.equals(content, that.content) &&
                Objects.equals(color, that.color) &&
                Objects.equals(pinned, that.pinned);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, color, pinned);
    }

    @Override
    public String toString() {
        return "NoteUpdateRequest(" +
                "content=" + content +
                ", color=" + color +
                ", pinned=" + pinned +
                ')';
    }
}
