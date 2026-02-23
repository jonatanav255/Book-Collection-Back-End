package com.bookshelf.dto;

import java.util.List;
import java.util.Objects;

public class PageTextWithTimings {
    private String text;
    private List<WordTiming> wordTimings;
    private String audioUrl;

    public PageTextWithTimings() {
    }

    public PageTextWithTimings(String text, List<WordTiming> wordTimings, String audioUrl) {
        this.text = text;
        this.wordTimings = wordTimings;
        this.audioUrl = audioUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<WordTiming> getWordTimings() {
        return wordTimings;
    }

    public void setWordTimings(List<WordTiming> wordTimings) {
        this.wordTimings = wordTimings;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageTextWithTimings that = (PageTextWithTimings) o;
        return Objects.equals(text, that.text) &&
                Objects.equals(wordTimings, that.wordTimings) &&
                Objects.equals(audioUrl, that.audioUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, wordTimings, audioUrl);
    }

    @Override
    public String toString() {
        return "PageTextWithTimings(" +
                "text=" + text +
                ", wordTimings=" + wordTimings +
                ", audioUrl=" + audioUrl +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text;
        private List<WordTiming> wordTimings;
        private String audioUrl;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder wordTimings(List<WordTiming> wordTimings) {
            this.wordTimings = wordTimings;
            return this;
        }

        public Builder audioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
            return this;
        }

        public PageTextWithTimings build() {
            return new PageTextWithTimings(text, wordTimings, audioUrl);
        }
    }
}
