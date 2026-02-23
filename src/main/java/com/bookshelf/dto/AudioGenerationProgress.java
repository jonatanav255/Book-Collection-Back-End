package com.bookshelf.dto;

import java.util.Objects;

public class AudioGenerationProgress {
    private String status; // RUNNING, COMPLETED, FAILED, CANCELLED
    private int currentPage;
    private int totalPages;
    private double progressPercentage;
    private String errorMessage;
    private long startedAt;
    private long completedAt;

    public AudioGenerationProgress() {
    }

    public AudioGenerationProgress(String status, int currentPage, int totalPages, double progressPercentage, String errorMessage, long startedAt, long completedAt) {
        this.status = status;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.progressPercentage = progressPercentage;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioGenerationProgress that = (AudioGenerationProgress) o;
        return currentPage == that.currentPage &&
                totalPages == that.totalPages &&
                Double.compare(that.progressPercentage, progressPercentage) == 0 &&
                startedAt == that.startedAt &&
                completedAt == that.completedAt &&
                Objects.equals(status, that.status) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, currentPage, totalPages, progressPercentage, errorMessage, startedAt, completedAt);
    }

    @Override
    public String toString() {
        return "AudioGenerationProgress(" +
                "status=" + status +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", progressPercentage=" + progressPercentage +
                ", errorMessage=" + errorMessage +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private int currentPage;
        private int totalPages;
        private double progressPercentage;
        private String errorMessage;
        private long startedAt;
        private long completedAt;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder currentPage(int currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder progressPercentage(double progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder startedAt(long startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(long completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public AudioGenerationProgress build() {
            return new AudioGenerationProgress(status, currentPage, totalPages, progressPercentage, errorMessage, startedAt, completedAt);
        }
    }
}
