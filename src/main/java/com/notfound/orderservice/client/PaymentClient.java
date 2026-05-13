package com.notfound.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.notfound.orderservice.model.dto.request.PaymentRequest;
import com.notfound.orderservice.model.dto.response.ApiResponse;
import com.notfound.orderservice.model.dto.response.CreatePaymentResponse;

@FeignClient(name = "payment-service", url = "${payment-service.url:http://payment-service:8080}")
public interface PaymentClient {

    @PostMapping("/api/v1/payment/vnpay/create")
    ApiResponse<CreatePaymentResponse> createVNPayPayment(@RequestBody PaymentRequest request);

    @PostMapping("/api/v1/payment/zalopay/create")
    ApiResponse<CreatePaymentResponse> createZaloPayPayment(@RequestBody PaymentRequest request);

    @PostMapping("/api/v1/payment/momo/create")
    ApiResponse<CreatePaymentResponse> createMoMoPayment(@RequestBody PaymentRequest request);
}
