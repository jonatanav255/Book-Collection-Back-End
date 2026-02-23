package com.bookshelf.dto;

import java.util.Objects;

public class LibraryStatsResponse {
    private long totalBooks;
    private long unreadBooks;
    private long readingBooks;
    private long finishedBooks;
    private long totalPages;
    private long totalPagesRead;
    private BookResponse continueReading;

    public LibraryStatsResponse() {
    }

    public LibraryStatsResponse(long totalBooks, long unreadBooks, long readingBooks, long finishedBooks, long totalPages, long totalPagesRead, BookResponse continueReading) {
        this.totalBooks = totalBooks;
        this.unreadBooks = unreadBooks;
        this.readingBooks = readingBooks;
        this.finishedBooks = finishedBooks;
        this.totalPages = totalPages;
        this.totalPagesRead = totalPagesRead;
        this.continueReading = continueReading;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(long totalBooks) {
        this.totalBooks = totalBooks;
    }

    public long getUnreadBooks() {
        return unreadBooks;
    }

    public void setUnreadBooks(long unreadBooks) {
        this.unreadBooks = unreadBooks;
    }

    public long getReadingBooks() {
        return readingBooks;
    }

    public void setReadingBooks(long readingBooks) {
        this.readingBooks = readingBooks;
    }

    public long getFinishedBooks() {
        return finishedBooks;
    }

    public void setFinishedBooks(long finishedBooks) {
        this.finishedBooks = finishedBooks;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalPagesRead() {
        return totalPagesRead;
    }

    public void setTotalPagesRead(long totalPagesRead) {
        this.totalPagesRead = totalPagesRead;
    }

    public BookResponse getContinueReading() {
        return continueReading;
    }

    public void setContinueReading(BookResponse continueReading) {
        this.continueReading = continueReading;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryStatsResponse that = (LibraryStatsResponse) o;
        return totalBooks == that.totalBooks &&
                unreadBooks == that.unreadBooks &&
                readingBooks == that.readingBooks &&
                finishedBooks == that.finishedBooks &&
                totalPages == that.totalPages &&
                totalPagesRead == that.totalPagesRead &&
                Objects.equals(continueReading, that.continueReading);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalBooks, unreadBooks, readingBooks, finishedBooks, totalPages, totalPagesRead, continueReading);
    }

    @Override
    public String toString() {
        return "LibraryStatsResponse(" +
                "totalBooks=" + totalBooks +
                ", unreadBooks=" + unreadBooks +
                ", readingBooks=" + readingBooks +
                ", finishedBooks=" + finishedBooks +
                ", totalPages=" + totalPages +
                ", totalPagesRead=" + totalPagesRead +
                ", continueReading=" + continueReading +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalBooks;
        private long unreadBooks;
        private long readingBooks;
        private long finishedBooks;
        private long totalPages;
        private long totalPagesRead;
        private BookResponse continueReading;

        public Builder totalBooks(long totalBooks) {
            this.totalBooks = totalBooks;
            return this;
        }

        public Builder unreadBooks(long unreadBooks) {
            this.unreadBooks = unreadBooks;
            return this;
        }

        public Builder readingBooks(long readingBooks) {
            this.readingBooks = readingBooks;
            return this;
        }

        public Builder finishedBooks(long finishedBooks) {
            this.finishedBooks = finishedBooks;
            return this;
        }

        public Builder totalPages(long totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder totalPagesRead(long totalPagesRead) {
            this.totalPagesRead = totalPagesRead;
            return this;
        }

        public Builder continueReading(BookResponse continueReading) {
            this.continueReading = continueReading;
            return this;
        }

        public LibraryStatsResponse build() {
            return new LibraryStatsResponse(totalBooks, unreadBooks, readingBooks, finishedBooks, totalPages, totalPagesRead, continueReading);
        }
    }
}
