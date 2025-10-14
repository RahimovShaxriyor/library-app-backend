package com.example.paymentservice.controller;

import com.example.paymentservice.dto.click.ClickRequestDto;
import com.example.paymentservice.dto.click.ClickResponseDto;
import com.example.paymentservice.service.ClickMerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/merchant/click")
@RequiredArgsConstructor
public class ClickMerchantController {

    private final ClickMerchantService clickMerchantService;

    @PostMapping("/prepare")
    public ResponseEntity<ClickResponseDto> prepare(@RequestBody ClickRequestDto request) {
        log.info("Click Prepare request received: {}", request);
        ClickResponseDto response = clickMerchantService.prepare(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    public ResponseEntity<ClickResponseDto> complete(@RequestBody ClickRequestDto request) {
        log.info("Click Complete request received: {}", request);
        ClickResponseDto response = clickMerchantService.complete(request);
        return ResponseEntity.ok(response);
    }
}
