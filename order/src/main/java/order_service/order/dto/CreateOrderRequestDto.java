package order_service.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequestDto {

    private static final int MAX_ITEMS_PER_ORDER = 20;
    private static final int MAX_TOTAL_QUANTITY = 100;
    private static final int MAX_QUANTITY_PER_ITEM = 50;

    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = MAX_ITEMS_PER_ORDER, message = "Order cannot contain more than {max} items")
    @Valid
    private List<OrderItemDto> items;

    // Дополнительные поля
    @Size(max = 500, message = "Shipping address cannot exceed {max} characters")
    private String shippingAddress;

    @Size(max = 1000, message = "Customer notes cannot exceed {max} characters")
    private String customerNotes;

    // Валидация бизнес-логики
    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        validateNoDuplicateBooks();
        validateTotalQuantity();
        validateIndividualQuantities();
    }

    private void validateNoDuplicateBooks() {
        Set<Long> uniqueBookIds = items.stream()
                .map(OrderItemDto::getBookId)
                .collect(Collectors.toSet());

        if (uniqueBookIds.size() != items.size()) {
            List<Long> duplicateBooks = findDuplicateBookIds();
            throw new IllegalArgumentException(
                    String.format("Order contains duplicate book items. Duplicate book IDs: %s", duplicateBooks)
            );
        }
    }

    private List<Long> findDuplicateBookIds() {
        return items.stream()
                .collect(Collectors.groupingBy(OrderItemDto::getBookId, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    private void validateTotalQuantity() {
        int totalQuantity = items.stream()
                .mapToInt(OrderItemDto::getQuantity)
                .sum();

        if (totalQuantity > MAX_TOTAL_QUANTITY) {
            throw new IllegalArgumentException(
                    String.format("Total quantity cannot exceed %d items per order. Current total: %d",
                            MAX_TOTAL_QUANTITY, totalQuantity)
            );
        }

        if (totalQuantity <= 0) {
            throw new IllegalArgumentException("Total quantity must be greater than 0");
        }
    }

    private void validateIndividualQuantities() {
        for (int i = 0; i < items.size(); i++) {
            OrderItemDto item = items.get(i);
            if (item.getQuantity() > MAX_QUANTITY_PER_ITEM) {
                throw new IllegalArgumentException(
                        String.format("Item %d: Quantity cannot exceed %d per book. Requested: %d",
                                i + 1, MAX_QUANTITY_PER_ITEM, item.getQuantity())
                );
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        String.format("Item %d: Quantity must be positive. Requested: %d",
                                i + 1, item.getQuantity())
                );
            }
        }
    }

    // Utility methods
    public int getTotalQuantity() {
        return items != null ? items.stream().mapToInt(OrderItemDto::getQuantity).sum() : 0;
    }

    public int getDistinctBookCount() {
        return items != null ? (int) items.stream().map(OrderItemDto::getBookId).distinct().count() : 0;
    }

    public boolean hasShippingAddress() {
        return shippingAddress != null && !shippingAddress.trim().isEmpty();
    }

    public boolean hasCustomerNotes() {
        return customerNotes != null && !customerNotes.trim().isEmpty();
    }

    // Static factory methods
    public static CreateOrderRequestDto createSimple(List<OrderItemDto> items) {
        return CreateOrderRequestDto.builder()
                .items(items)
                .build();
    }

    public static CreateOrderRequestDto createWithShipping(List<OrderItemDto> items, String shippingAddress) {
        return CreateOrderRequestDto.builder()
                .items(items)
                .shippingAddress(shippingAddress)
                .build();
    }

    public static CreateOrderRequestDto createFull(List<OrderItemDto> items, String shippingAddress, String customerNotes) {
        return CreateOrderRequestDto.builder()
                .items(items)
                .shippingAddress(shippingAddress)
                .customerNotes(customerNotes)
                .build();
    }

    // Logging method for debugging
    public void logOrderDetails() {
        if (log.isDebugEnabled()) {
            log.debug("Order details - Items: {}, Total quantity: {}, Has shipping: {}, Has notes: {}",
                    items.size(), getTotalQuantity(), hasShippingAddress(), hasCustomerNotes());
        }
    }
}