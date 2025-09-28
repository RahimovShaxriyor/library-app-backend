package order_service.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "books")
public class Book {

    @Id
    @SequenceGenerator(
            name = "book_seq",
            sequenceName = "BOOK_SEQ",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "book_seq"
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String season;

    @Column(nullable = false)
    private Integer pages;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus stockStatus; // Переименовано для ясности

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookAvailabilityStatus availabilityStatus; // Статус доступности для продажи

    @Version
    private Integer version; // Для оптимистической блокировки

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateStockStatus() {
        if (this.quantity != null && this.quantity > 0) {
            this.stockStatus = StockStatus.IN_STOCK;
        } else {
            this.stockStatus = StockStatus.SOLD_OUT;
        }
    }
}