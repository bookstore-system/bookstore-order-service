package com.hamtech.bookstoreorderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.hamtech.bookstoreorderservice.model.dto.response.ApiResponse;

import java.util.Map;

// Mocks the book service communication to fetch book details
@FeignClient(name = "book-service", url = "${book-service.url:http://book-service:8080}")
public interface BookClient {
    
    @GetMapping("/api/books/{id}")
    ApiResponse<Map<String, Object>> getBookById(@PathVariable("id") String id);
}
