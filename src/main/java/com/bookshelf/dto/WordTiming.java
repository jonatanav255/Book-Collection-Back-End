package com.bookshelf.dto;

import java.util.Objects;

public class WordTiming {
    private String word;
    private double startTime; // in seconds
    private double endTime;   // in seconds

    public WordTiming() {
    }

    public WordTiming(String word, double startTime, double endTime) {
        this.word = word;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WordTiming that = (WordTiming) o;
        return Double.compare(that.startTime, startTime) == 0 &&
                Double.compare(that.endTime, endTime) == 0 &&
                Objects.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, startTime, endTime);
    }

    @Override
    public String toString() {
        return "WordTiming(" +
                "word=" + word +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String word;
        private double startTime;
        private double endTime;

        public Builder word(String word) {
            this.word = word;
            return this;
        }

        public Builder startTime(double startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(double endTime) {
            this.endTime = endTime;
            return this;
        }

        public WordTiming build() {
            return new WordTiming(word, startTime, endTime);
        }
    }
}
