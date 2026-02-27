package com.bookshelf.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BulkDeleteRequest {

    @NotEmpty
    @Size(max = 100)
    private List<UUID> ids;

    public BulkDeleteRequest() {
    }

    public BulkDeleteRequest(List<UUID> ids) {
        this.ids = ids;
    }

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkDeleteRequest that = (BulkDeleteRequest) o;
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids);
    }

    @Override
    public String toString() {
        return "BulkDeleteRequest(ids=" + ids + ')';
    }
}
