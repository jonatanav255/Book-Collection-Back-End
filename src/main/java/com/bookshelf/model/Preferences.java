package com.bookshelf.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "preferences")
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50, nullable = false)
    private String theme = "light";

    @Column(name = "font_family", length = 100)
    private String fontFamily = "serif";

    @Column(name = "font_size", length = 10)
    private String fontSize = "md";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // No-arg constructor
    public Preferences() {
    }

    // All-args constructor
    public Preferences(UUID id, String theme, String fontFamily, String fontSize,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.theme = theme;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder

    public static PreferencesBuilder builder() {
        return new PreferencesBuilder();
    }

    public static class PreferencesBuilder {
        private UUID id;
        private String theme = "light";
        private String fontFamily = "serif";
        private String fontSize = "md";
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        PreferencesBuilder() {
        }

        public PreferencesBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PreferencesBuilder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public PreferencesBuilder fontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
            return this;
        }

        public PreferencesBuilder fontSize(String fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public PreferencesBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PreferencesBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Preferences build() {
            Preferences preferences = new Preferences();
            preferences.id = this.id;
            preferences.theme = this.theme;
            preferences.fontFamily = this.fontFamily;
            preferences.fontSize = this.fontSize;
            preferences.createdAt = this.createdAt;
            preferences.updatedAt = this.updatedAt;
            return preferences;
        }
    }
}
