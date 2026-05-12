package com.notfound.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.notfound.orderservice.client.dto.AddressResponse;
import com.notfound.orderservice.model.dto.response.ApiResponse;

@FeignClient(name = "user-service", url = "${user-service.url:http://user-service:8080}")
public interface UserClient {

    @GetMapping("/api/v1/users/{userId}/addresses/{addressId}")
    ApiResponse<AddressResponse> getUserAddress(
            @PathVariable("userId") String userId,
            @PathVariable("addressId") String addressId
    );
}