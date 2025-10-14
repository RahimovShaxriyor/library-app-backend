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
    @SequenceGenerator(name = "book_seq", sequenceName = "BOOKS_SEQ", allocationSize = 1) // Изменено на BOOKS_SEQ и allocationSize = 50
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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    private String genre;
    private String publisher;
    private String isbn;

    @Column(name = "publication_year")
    private Integer publicationYear;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status")
    private StockStatus stockStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status")
    private BookAvailabilityStatus availabilityStatus;

    @Version
    @Column(name = "version")
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
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