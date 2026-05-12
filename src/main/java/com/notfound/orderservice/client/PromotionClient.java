package com.notfound.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.notfound.orderservice.client.dto.PromotionApplyRequest;
import com.notfound.orderservice.client.dto.PromotionApplyResponse;
import com.notfound.orderservice.model.dto.response.ApiResponse;

@FeignClient(name = "promotion-service", url = "${promotion-service.url:http://promotion-service:8080}")
public interface PromotionClient {

    @PostMapping("/api/v1/promotions/apply")
    ApiResponse<PromotionApplyResponse> applyPromotion(@RequestBody PromotionApplyRequest request);
}