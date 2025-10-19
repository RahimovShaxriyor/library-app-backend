package order_service.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @SequenceGenerator(
            name = "order_item_seq",
            sequenceName = "ORDER_ITEM_SEQ",
            allocationSize = 50
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_item_seq"
    )
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Long bookId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}