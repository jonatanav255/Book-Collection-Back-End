package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BulkStatusUpdateRequest {

    @NotEmpty
    @Size(max = 100)
    private List<UUID> ids;

    @NotNull
    private ReadingStatus status;

    public BulkStatusUpdateRequest() {
    }

    public BulkStatusUpdateRequest(List<UUID> ids, ReadingStatus status) {
        this.ids = ids;
        this.status = status;
    }

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
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
        BulkStatusUpdateRequest that = (BulkStatusUpdateRequest) o;
        return Objects.equals(ids, that.ids) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, status);
    }

    @Override
    public String toString() {
        return "BulkStatusUpdateRequest(ids=" + ids + ", status=" + status + ')';
    }
}
