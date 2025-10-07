package order_service.order.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.order.dto.CreateOrderRequestDto;
import order_service.order.dto.OrderItemDto;
import order_service.order.dto.OrderResponseDto;
import order_service.order.exceptions.ResourceNotFoundException;
import order_service.order.exceptions.ValidationException;
import order_service.order.mappers.OrderMapper;
import order_service.order.model.*;
import order_service.order.repositories.BookRepository;
import order_service.order.repositories.OrderRepository;
import order_service.order.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final OrderMapper orderMapper;

    private static final BigDecimal MINIMUM_ORDER_AMOUNT = new BigDecimal("50000.0");
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PAID
    );

    @Override
    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto request, UserDetails currentUser) {
        log.info("Creating order for user: {}", currentUser.getUsername());

        validateOrderRequest(request);
        request.validate();

        String userEmailAsId = currentUser.getUsername();

        Order order = buildOrder(userEmailAsId);
        List<OrderItem> orderItems = processOrderItems(request.getItems(), order);

        BigDecimal totalPrice = calculateTotalPrice(orderItems);
        validateMinimumOrderAmount(totalPrice);

        order.setItems(orderItems);
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} for user: {}", savedOrder.getId(), userEmailAsId);

        return orderMapper.toOrderResponseDto(savedOrder);
    }

    @Override
    public Page<OrderResponseDto> getOrdersForUser(UserDetails currentUser, Pageable pageable) {
        String userEmailAsId = currentUser.getUsername();
        log.debug("Fetching orders for user: {}", userEmailAsId);

        Page<Order> userOrdersPage = orderRepository.findByUserId(userEmailAsId, pageable);
        return userOrdersPage.map(orderMapper::toOrderResponseDto);
    }

    @Override
    @Transactional
    public OrderResponseDto processPayment(Long orderId, UserDetails currentUser) {
        log.info("Processing payment for order: {} by user: {}", orderId, currentUser.getUsername());

        Order order = getOrderById(orderId);
        validateOrderAccess(order, currentUser);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ValidationException(
                    "Only orders with PENDING status can be paid. Current status: " + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        log.info("Payment processed successfully for order: {}", orderId);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto shipOrder(Long orderId) {
        log.info("Shipping order: {}", orderId);

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ValidationException(
                    "Only PAID orders can be shipped. Current status: " + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order shipped successfully: {}", orderId);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto deliverOrder(Long orderId) {
        log.info("Delivering order: {}", orderId);

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ValidationException(
                    "Only SHIPPED orders can be delivered. Current status: " + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order delivered successfully: {}", orderId);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, UserDetails currentUser) {
        log.info("Cancelling order: {} by user: {}", orderId, currentUser.getUsername());

        Order order = getOrderById(orderId);
        validateOrderAccess(order, currentUser);

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new ValidationException(
                    "Cannot cancel order with status: " + order.getStatus() +
                            ". Only PENDING or PAID orders can be cancelled."
            );
        }

        restoreBookQuantities(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order cancelled successfully: {}", orderId);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    public OrderResponseDto getOrderDetails(Long orderId, UserDetails currentUser) {
        log.debug("Fetching order details for order: {} by user: {}", orderId, currentUser.getUsername());

        Order order = getOrderById(orderId);
        validateOrderAccess(order, currentUser);

        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    public Page<OrderResponseDto> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders with pagination");
        return orderRepository.findAll(pageable).map(orderMapper::toOrderResponseDto);
    }

    @Override
    public Page<OrderResponseDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Fetching orders by status: {}", status);
        return orderRepository.findByStatus(status, pageable).map(orderMapper::toOrderResponseDto);
    }

    private void validateOrderRequest(CreateOrderRequestDto request) {
        if (request == null) {
            throw new ValidationException("Order request cannot be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("Order must contain at least one item");
        }
    }

    private Order buildOrder(String userId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private List<OrderItem> processOrderItems(List<OrderItemDto> itemDtos, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDto itemDto : itemDtos) {
            itemDto.validate();

            Book book = bookRepository.findByIdForUpdate(itemDto.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Book not found with id: " + itemDto.getBookId()
                    ));

            validateBookAvailability(book, itemDto.getQuantity());
            updateBookQuantity(book, itemDto.getQuantity());

            OrderItem orderItem = buildOrderItem(order, book, itemDto);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void validateBookAvailability(Book book, Integer quantity) {
        if (book.getAvailabilityStatus() != BookAvailabilityStatus.ACTIVE) {
            throw new ValidationException("Book '" + book.getName() + "' is not available for purchase");
        }

        if (book.getQuantity() < quantity) {
            throw new ValidationException(
                    "Not enough stock for book: " + book.getName() +
                            ". Available: " + book.getQuantity() + ", Requested: " + quantity
            );
        }
    }

    private void updateBookQuantity(Book book, Integer quantity) {
        book.setQuantity(book.getQuantity() - quantity);
        bookRepository.save(book);
    }

    private OrderItem buildOrderItem(Order order, Book book, OrderItemDto itemDto) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setBookId(book.getId());
        orderItem.setName(book.getName());
        orderItem.setQuantity(itemDto.getQuantity());
        orderItem.setPrice(book.getPrice());
        return orderItem;
    }

    private BigDecimal calculateTotalPrice(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateMinimumOrderAmount(BigDecimal totalPrice) {
        if (totalPrice.compareTo(MINIMUM_ORDER_AMOUNT) < 0) {
            throw new ValidationException(
                    "Order total price (" + totalPrice + ") is below the minimum required amount of " + MINIMUM_ORDER_AMOUNT
            );
        }
    }

    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void validateOrderAccess(Order order, UserDetails currentUser) {
        boolean isOwner = order.getUserId().equals(currentUser.getUsername());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Access denied to order: " + order.getId());
        }
    }

    private void restoreBookQuantities(Order order) {
        for (OrderItem item : order.getItems()) {
            Book book = bookRepository.findByIdForUpdate(item.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Book not found with id: " + item.getBookId()
                    ));

            book.setQuantity(book.getQuantity() + item.getQuantity());
            bookRepository.save(book);
        }
    }
}
