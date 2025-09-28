package order_service.order.dto;

import lombok.Data;
import order_service.order.model.Status;
import java.time.LocalDateTime;
@Data
public class BookResponseDto {

    private Long id;
    private String name;
    private String season;
    private Integer pages;
    private Double price;
    private Integer quantity;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}