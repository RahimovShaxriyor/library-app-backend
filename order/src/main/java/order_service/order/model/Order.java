package order_service.order.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Order {

    @Id
    @SequenceGenerator(
            name = "order_seq",
            sequenceName = "ORDER_SEQ",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_seq"
    )
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "order_number", unique = true, length = 20)
    private String orderNumber;

    public void addOrderItem(OrderItem item) {
        if (items == null) items = new ArrayList<>();
        items.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        if (items != null) {
            items.remove(item);
            item.setOrder(null);
        }
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.PAID;
    }

    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.deliveredAt = shippedAt;
        if (shippedAt != null) {
            this.status = OrderStatus.DELIVERED;
        }
    }

    // ================== toString ==================
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", version=" + version +
                ", userId='" + userId + '\'' +
                ", totalPrice=" + totalPrice +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", paidAt=" + paidAt +
                ", deliveredAt=" + deliveredAt +
                ", cancelledAt=" + cancelledAt +
                ", orderNumber='" + orderNumber + '\'' +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}
