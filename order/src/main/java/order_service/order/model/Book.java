package order_service.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "books")
@SQLRestriction("availability_status != 'DELETED'")
public class Book {

    @Id
    @SequenceGenerator(name = "book_seq", sequenceName = "BOOK_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(length = 1000)
    private String description;

    private String season;
    private Integer pages;
    private BigDecimal price;
    private Integer quantity;
    private String genre;
    private String publisher;
    private String isbn;
    private Integer publicationYear;
    private String language;

    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus;

    @Enumerated(EnumType.STRING)
    private BookAvailabilityStatus availabilityStatus;

    @Version
    private Integer version;

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

    public String getName() {
        return this.title;
    }
}

