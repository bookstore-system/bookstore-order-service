package com.notfound.orderservice.client.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookBatchLookupResponse {
    private List<BookBatchLookupItemResponse> items;
    private List<String> missingIds;
}
