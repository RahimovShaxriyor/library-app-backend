package com.example.paymentservice.service;

import com.example.paymentservice.dto.click.ClickRequestDto;
import com.example.paymentservice.dto.click.ClickResponseDto;

public interface ClickMerchantService {
    ClickResponseDto prepare(ClickRequestDto request);
    ClickResponseDto complete(ClickRequestDto request);
}
