package com.notfound.orderservice.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.notfound.orderservice.client.dto.BookBatchRequest;
import com.notfound.orderservice.client.dto.BookDetailResponse;
import com.notfound.orderservice.client.dto.ReduceStockRequest;
import com.notfound.orderservice.model.dto.response.ApiResponse;

@FeignClient(name = "book-service", url = "${book-service.url:http://book-service:8080}")
public interface BookClient {
    
    @PostMapping("/api/v1/books/batch-details")
    ApiResponse<List<BookDetailResponse>> getBatchBookDetails(@RequestBody BookBatchRequest request);

    @PostMapping("/api/v1/books/reduce-stock")
    ApiResponse<Void> reduceStock(@RequestBody ReduceStockRequest request);

    @PostMapping("/api/v1/books/restore-stock")
    ApiResponse<Void> restoreStock(@RequestBody ReduceStockRequest request);
}
