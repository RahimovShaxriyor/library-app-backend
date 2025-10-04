package order_service.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OrderItemDto {

    private static final int MAX_QUANTITY_PER_ITEM = 50;
    private static final int MIN_QUANTITY = 1;

    @NotNull(message = "Book ID cannot be null")
    @Positive(message = "Book ID must be a positive number")
    private Long bookId;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = MIN_QUANTITY, message = "Quantity must be at least {value}")
    @Max(value = MAX_QUANTITY_PER_ITEM, message = "Quantity cannot exceed {value}")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantity;

    // Дополнительные поля для расширенной функциональности
    private String giftMessage;

    private Boolean isGiftWrapped;

    private String customNote;

    // Бизнес-валидация
    public void validate() {
        if (quantity != null && quantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException(
                    String.format("Quantity cannot exceed %d per book. Requested: %d",
                            MAX_QUANTITY_PER_ITEM, quantity)
            );
        }

        if (quantity != null && quantity < MIN_QUANTITY) {
            throw new IllegalArgumentException(
                    String.format("Quantity must be at least %d. Requested: %d",
                            MIN_QUANTITY, quantity)
            );
        }

        if (bookId != null && bookId <= 0) {
            throw new IllegalArgumentException("Book ID must be a positive number");
        }

        validateGiftMessage();
    }

    private void validateGiftMessage() {
        if (giftMessage != null && giftMessage.length() > 500) {
            throw new IllegalArgumentException("Gift message cannot exceed 500 characters");
        }

        if (customNote != null && customNote.length() > 1000) {
            throw new IllegalArgumentException("Custom note cannot exceed 1000 characters");
        }
    }

    // Utility methods
    public boolean hasGiftMessage() {
        return giftMessage != null && !giftMessage.trim().isEmpty();
    }

    public boolean hasCustomNote() {
        return customNote != null && !customNote.trim().isEmpty();
    }

    public boolean requiresGiftWrapping() {
        return Boolean.TRUE.equals(isGiftWrapped) || hasGiftMessage();
    }

    // Static factory methods for convenient object creation
    public static OrderItemDto of(Long bookId, Integer quantity) {
        return OrderItemDto.builder()
                .bookId(bookId)
                .quantity(quantity)
                .build();
    }

    public static OrderItemDto of(Long bookId, Integer quantity, String giftMessage) {
        return OrderItemDto.builder()
                .bookId(bookId)
                .quantity(quantity)
                .giftMessage(giftMessage)
                .isGiftWrapped(true)
                .build();
    }

    public static OrderItemDto of(Long bookId, Integer quantity, String giftMessage, String customNote) {
        return OrderItemDto.builder()
                .bookId(bookId)
                .quantity(quantity)
                .giftMessage(giftMessage)
                .customNote(customNote)
                .isGiftWrapped(true)
                .build();
    }

    public static OrderItemDto createWithGiftWrapping(Long bookId, Integer quantity) {
        return OrderItemDto.builder()
                .bookId(bookId)
                .quantity(quantity)
                .isGiftWrapped(true)
                .build();
    }

    // Copy constructor
    public static OrderItemDto copyOf(OrderItemDto original) {
        return OrderItemDto.builder()
                .bookId(original.getBookId())
                .quantity(original.getQuantity())
                .giftMessage(original.getGiftMessage())
                .isGiftWrapped(original.getIsGiftWrapped())
                .customNote(original.getCustomNote())
                .build();
    }

    // Method to log item details (useful for debugging)
    public void logDetails() {
        if (log.isDebugEnabled()) {
            log.debug("OrderItem - BookId: {}, Quantity: {}, GiftWrap: {}, HasMessage: {}, HasNote: {}",
                    bookId, quantity, isGiftWrapped, hasGiftMessage(), hasCustomNote());
        }
    }

    // Validation method for service layer
    public void validateForOrder() {
        validate();

        // Additional business rules
        if (isGiftWrapped != null && isGiftWrapped && quantity > 10) {
            throw new IllegalArgumentException(
                    "Gift wrapping is not available for quantities greater than 10"
            );
        }
    }

    // Price calculation helper (if price is available from service)
    public Double calculateItemPrice(Double unitPrice) {
        if (unitPrice == null || quantity == null) {
            return 0.0;
        }
        return unitPrice * quantity;
    }

    @Override
    public String toString() {
        return String.format("OrderItemDto{bookId=%d, quantity=%d, giftWrap=%s}",
                bookId, quantity, isGiftWrapped);
    }
}