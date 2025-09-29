package com.example.paymentservice.service;

import java.math.BigDecimal;
import java.util.List;

public interface OrderServiceClient {

    record ItemDetail(String title, BigDecimal price, Integer count, String code, String package_code, Integer vat_percent) {}
    record OrderDetail(List<ItemDetail> items) {}

    OrderDetail getOrderDetailsForFiscalization(Long orderId);
}
