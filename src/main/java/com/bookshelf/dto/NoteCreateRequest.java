package com.bookshelf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public class NoteCreateRequest {
    @NotNull(message = "Page number is required")
    private Integer pageNumber;

    @NotBlank(message = "Content is required")
    private String content;

    private String color;
    private Boolean pinned = false;

    public NoteCreateRequest() {
    }

    public NoteCreateRequest(Integer pageNumber, String content, String color, Boolean pinned) {
        this.pageNumber = pageNumber;
        this.content = content;
        this.color = color;
        this.pinned = pinned;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteCreateRequest that = (NoteCreateRequest) o;
        return Objects.equals(pageNumber, that.pageNumber) &&
                Objects.equals(content, that.content) &&
                Objects.equals(color, that.color) &&
                Objects.equals(pinned, that.pinned);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, content, color, pinned);
    }

    @Override
    public String toString() {
        return "NoteCreateRequest(" +
                "pageNumber=" + pageNumber +
                ", content=" + content +
                ", color=" + color +
                ", pinned=" + pinned +
                ')';
    }
}
