package com.bookshelf.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse {
    private List<BookItem> items;

    public List<BookItem> getItems() {
        return items;
    }

    public void setItems(List<BookItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleBooksResponse that = (GoogleBooksResponse) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        return "GoogleBooksResponse(" +
                "items=" + items +
                ')';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookItem {
        private VolumeInfo volumeInfo;

        public VolumeInfo getVolumeInfo() {
            return volumeInfo;
        }

        public void setVolumeInfo(VolumeInfo volumeInfo) {
            this.volumeInfo = volumeInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookItem bookItem = (BookItem) o;
            return Objects.equals(volumeInfo, bookItem.volumeInfo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(volumeInfo);
        }

        @Override
        public String toString() {
            return "BookItem(" +
                    "volumeInfo=" + volumeInfo +
                    ')';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String description;
        private List<String> categories;
        private ImageLinks imageLinks;
        private Integer pageCount;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getAuthors() {
            return authors;
        }

        public void setAuthors(List<String> authors) {
            this.authors = authors;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getCategories() {
            return categories;
        }

        public void setCategories(List<String> categories) {
            this.categories = categories;
        }

        public ImageLinks getImageLinks() {
            return imageLinks;
        }

        public void setImageLinks(ImageLinks imageLinks) {
            this.imageLinks = imageLinks;
        }

        public Integer getPageCount() {
            return pageCount;
        }

        public void setPageCount(Integer pageCount) {
            this.pageCount = pageCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VolumeInfo that = (VolumeInfo) o;
            return Objects.equals(title, that.title) &&
                    Objects.equals(authors, that.authors) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(categories, that.categories) &&
                    Objects.equals(imageLinks, that.imageLinks) &&
                    Objects.equals(pageCount, that.pageCount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, authors, description, categories, imageLinks, pageCount);
        }

        @Override
        public String toString() {
            return "VolumeInfo(" +
                    "title=" + title +
                    ", authors=" + authors +
                    ", description=" + description +
                    ", categories=" + categories +
                    ", imageLinks=" + imageLinks +
                    ", pageCount=" + pageCount +
                    ')';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinks {
        private String thumbnail;
        private String smallThumbnail;

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getSmallThumbnail() {
            return smallThumbnail;
        }

        public void setSmallThumbnail(String smallThumbnail) {
            this.smallThumbnail = smallThumbnail;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageLinks that = (ImageLinks) o;
            return Objects.equals(thumbnail, that.thumbnail) &&
                    Objects.equals(smallThumbnail, that.smallThumbnail);
        }

        @Override
        public int hashCode() {
            return Objects.hash(thumbnail, smallThumbnail);
        }

        @Override
        public String toString() {
            return "ImageLinks(" +
                    "thumbnail=" + thumbnail +
                    ", smallThumbnail=" + smallThumbnail +
                    ')';
        }
    }
}
