package com.bookshelf.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BulkOperationResponse {

    private int successCount;
    private int failureCount;
    private List<UUID> failedIds;

    public BulkOperationResponse() {
    }

    public BulkOperationResponse(int successCount, int failureCount, List<UUID> failedIds) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.failedIds = failedIds;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<UUID> getFailedIds() {
        return failedIds;
    }

    public void setFailedIds(List<UUID> failedIds) {
        this.failedIds = failedIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkOperationResponse that = (BulkOperationResponse) o;
        return successCount == that.successCount &&
                failureCount == that.failureCount &&
                Objects.equals(failedIds, that.failedIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successCount, failureCount, failedIds);
    }

    @Override
    public String toString() {
        return "BulkOperationResponse(successCount=" + successCount +
                ", failureCount=" + failureCount +
                ", failedIds=" + failedIds + ')';
    }
}
