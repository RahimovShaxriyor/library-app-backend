package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(@Valid @RequestBody PaymentInitiationRequest request) {
        PaymentInitiationResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback/{provider}")
    public ResponseEntity<Void> handleCallback(@PathVariable String provider, @RequestBody Map<String, String> callbackData) {
        paymentService.handlePaymentCallback(provider, callbackData);
        return ResponseEntity.ok().build();
    }
}
