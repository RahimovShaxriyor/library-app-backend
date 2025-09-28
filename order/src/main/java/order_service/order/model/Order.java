package order_service.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
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

    @Column(nullable = false)
    private String userId;

    @OneToMany(mappedBy = "order", cascade = jakarta.persistence.CascadeType.ALL)
    private List<OrderItem> items;

    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}