package order_service.order.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    @NotNull(message = "Book ID is required")
    @Positive(message = "Book ID must be positive")
    private Long bookId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    // Валидация бизнес-логики
    public void validate() {
        if (quantity != null && quantity > 50) {
            throw new IllegalArgumentException("Quantity per book cannot exceed 50");
        }
        if (quantity != null && quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    // Static factory method
    public static OrderItemDto of(Long bookId, Integer quantity) {
        return OrderItemDto.builder()
                .bookId(bookId)
                .quantity(quantity)
                .build();
    }
}