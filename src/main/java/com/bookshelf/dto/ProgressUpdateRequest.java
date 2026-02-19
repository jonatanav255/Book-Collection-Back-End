package com.bookshelf.dto;

import com.bookshelf.model.ReadingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdateRequest {
    private Integer currentPage;
    private ReadingStatus status;
}
