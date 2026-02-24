package com.bookshelf.dto;

import jakarta.validation.constraints.Size;

import java.util.Objects;

public class PreferencesUpdateRequest {
    @Size(max = 50)
    private String theme;

    @Size(max = 100)
    private String fontFamily;

    @Size(max = 10)
    private String fontSize;

    public PreferencesUpdateRequest() {
    }

    public PreferencesUpdateRequest(String theme, String fontFamily, String fontSize) {
        this.theme = theme;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
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
        PreferencesUpdateRequest that = (PreferencesUpdateRequest) o;
        return Objects.equals(theme, that.theme) &&
                Objects.equals(fontFamily, that.fontFamily) &&
                Objects.equals(fontSize, that.fontSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theme, fontFamily, fontSize);
    }

    @Override
    public String toString() {
        return "PreferencesUpdateRequest(" +
                "theme=" + theme +
                ", fontFamily=" + fontFamily +
                ", fontSize=" + fontSize +
                ')';
    }
}
