package order_service.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import order_service.order.model.BookAvailabilityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDto {

    private Long id;
    private String name;
    private String author;
    private String season;
    private Integer pages;
    private BigDecimal price;
    private Integer quantity;
    private String description;
    private String genre;
    private String publisher;
    private String isbn;
    private Integer publicationYear;
    private String language;
    private BookAvailabilityStatus availabilityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Utility methods
    public boolean isAvailable() {
        return availabilityStatus == BookAvailabilityStatus.ACTIVE && quantity > 0;
    }

    public boolean isLowStock() {
        return quantity <= 5;
    }

    public boolean isOutOfStock() {
        return quantity == 0;
    }

    public BigDecimal calculateTotalValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public String getFullTitle() {
        return name + " - " + season;
    }
}