package com.example.paymentservice.controller;

import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;
import com.example.paymentservice.service.PaymeMerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/merchant/payme")
@RequiredArgsConstructor
public class PaymeMerchantController {

    private final PaymeMerchantService paymeMerchantService;

    @PostMapping
    public ResponseEntity<PaymeResponse> handlePaymeRequest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PaymeRequest request) {

        PaymeResponse response = paymeMerchantService.handleRequest(authorization, request);
        return ResponseEntity.ok(response);
    }
}

