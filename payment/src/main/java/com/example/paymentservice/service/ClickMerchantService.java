package com.example.paymentservice.service;

import com.example.paymentservice.dto.click.ClickRequestDto;
import com.example.paymentservice.dto.click.ClickResponseDto;

import java.util.Map;

public interface ClickMerchantService {

    ClickResponseDto prepare(ClickRequestDto request, String clientIp);

    ClickResponseDto complete(ClickRequestDto request, String clientIp);

    boolean verifySignature(ClickRequestDto request);

    boolean validateIpAddress(String clientIp);

    Map<String, Object> getHealthInfo();

    ClickResponseDto refund(ClickRequestDto request, String clientIp);

    Map<String, Object> getTransactionStatus(String clickTransId);

    void cleanupOldPrepareRecords();

    Map<String, Object> getClickStatistics(int days);

    boolean isDuplicateRequest(String clickTransId, Long signTime);
}