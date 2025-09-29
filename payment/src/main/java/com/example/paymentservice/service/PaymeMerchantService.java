package com.example.paymentservice.service;

import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;

public interface PaymeMerchantService {
    PaymeResponse handleRequest(String authorization, PaymeRequest request);
}
