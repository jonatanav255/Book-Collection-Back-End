package com.bookshelf.dto;

import java.util.Objects;
import java.util.UUID;

public class PreferencesResponse {
    private UUID id;
    private String theme;
    private String fontFamily;
    private String fontSize;

    public PreferencesResponse() {
    }

    public PreferencesResponse(UUID id, String theme, String fontFamily, String fontSize) {
        this.id = id;
        this.theme = theme;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreferencesResponse that = (PreferencesResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(theme, that.theme) &&
                Objects.equals(fontFamily, that.fontFamily) &&
                Objects.equals(fontSize, that.fontSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, theme, fontFamily, fontSize);
    }

    @Override
    public String toString() {
        return "PreferencesResponse(" +
                "id=" + id +
                ", theme=" + theme +
                ", fontFamily=" + fontFamily +
                ", fontSize=" + fontSize +
                ')';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String theme;
        private String fontFamily;
        private String fontSize;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public Builder fontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
            return this;
        }

        public Builder fontSize(String fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public PreferencesResponse build() {
            return new PreferencesResponse(id, theme, fontFamily, fontSize);
        }
    }
}
