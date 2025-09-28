package order_service.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookRequestDto {

    @NotBlank(message = "Book name cannot be blank")
    @Size(min = 2, max = 255, message = "Book name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Season/part cannot be blank")
    private String season;

    @NotNull(message = "Number of pages cannot be null")
    @Positive(message = "Number of pages must be positive")
    private Integer pages;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be a positive value")
    private Double price;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;
}