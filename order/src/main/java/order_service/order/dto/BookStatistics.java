package order_service.order.dto;

public record BookStatistics(
        long totalBooks,
        long activeBooks,
        long inactiveBooks,
        long outOfStockBooks,
        long lowStockBooks
) {}
