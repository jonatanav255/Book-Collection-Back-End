package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;
import jakarta.validation.constraints.Min;

import java.util.Objects;

public class ProgressUpdateRequest {
    @Min(0)
    private Integer currentPage;
    private ReadingStatus status;

    public ProgressUpdateRequest() {
    }

    public ProgressUpdateRequest(Integer currentPage, ReadingStatus status) {
        this.currentPage = currentPage;
        this.status = status;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgressUpdateRequest that = (ProgressUpdateRequest) o;
        return Objects.equals(currentPage, that.currentPage) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentPage, status);
    }

    @Override
    public String toString() {
        return "ProgressUpdateRequest(" +
                "currentPage=" + currentPage +
                ", status=" + status +
                ')';
    }
}
