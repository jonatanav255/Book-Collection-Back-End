package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;

import java.util.Objects;

public class BookUpdateRequest {
    private String title;
    private String author;
    private String description;
    private String genre;
    private ReadingStatus status;
    private String coverUrl;
    private Integer currentPage;

    public BookUpdateRequest() {
    }

    public BookUpdateRequest(String title, String author, String description, String genre, ReadingStatus status, String coverUrl, Integer currentPage) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.genre = genre;
        this.status = status;
        this.coverUrl = coverUrl;
        this.currentPage = currentPage;
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

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookUpdateRequest that = (BookUpdateRequest) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(author, that.author) &&
                Objects.equals(description, that.description) &&
                Objects.equals(genre, that.genre) &&
                status == that.status &&
                Objects.equals(coverUrl, that.coverUrl) &&
                Objects.equals(currentPage, that.currentPage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, description, genre, status, coverUrl, currentPage);
    }

    @Override
    public String toString() {
        return "BookUpdateRequest(" +
                "title=" + title +
                ", author=" + author +
                ", description=" + description +
                ", genre=" + genre +
                ", status=" + status +
                ", coverUrl=" + coverUrl +
                ", currentPage=" + currentPage +
                ')';
    }
}
