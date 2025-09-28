package order_service.order.services.impl;

import order_service.order.dto.CreateOrderRequestDto;
import order_service.order.dto.OrderResponseDto;
import order_service.order.exceptions.ResourceNotFoundException;
import order_service.order.exceptions.ValidationException;
import order_service.order.mappers.OrderMapper;
import order_service.order.model.*;
import order_service.order.repositories.BookRepository;
import order_service.order.repositories.OrderRepository;
import order_service.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final OrderMapper orderMapper;

    private static final double MINIMUM_ORDER_AMOUNT = 50000.0;

    @Override
    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto request, UserDetails currentUser) {
        String userEmailAsId = currentUser.getUsername();

        Order order = new Order();
        order.setUserId(userEmailAsId);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = new ArrayList<>();
        double totalPrice = 0.0;

        for (var itemDto : request.getItems()) {
            Book book = bookRepository.findByIdForUpdate(itemDto.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + itemDto.getBookId()));

            if (book.getAvailabilityStatus() != BookAvailabilityStatus.ACTIVE) {
                throw new ValidationException("Book '" + book.getName() + "' is currently not available for purchase.");
            }

            if (book.getQuantity() < itemDto.getQuantity()) {
                throw new ValidationException("Not enough stock for book: " + book.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBookId(book.getId());
            orderItem.setName(book.getName());
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(book.getPrice());

            orderItems.add(orderItem);
            totalPrice += book.getPrice() * itemDto.getQuantity();

            book.setQuantity(book.getQuantity() - itemDto.getQuantity());
            bookRepository.save(book);
        }

        order.setItems(orderItems);
        order.setTotalPrice(totalPrice);

        if (totalPrice < MINIMUM_ORDER_AMOUNT) {
            throw new ValidationException("Order total price is below the minimum required amount of " + MINIMUM_ORDER_AMOUNT);
        }

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponseDto(savedOrder);
    }

    @Override
    public Page<OrderResponseDto> getOrdersForUser(UserDetails currentUser, Pageable pageable) {
        String userEmailAsId = currentUser.getUsername();
        Page<Order> userOrdersPage = orderRepository.findByUserId(userEmailAsId, pageable);

        return userOrdersPage.map(orderMapper::toOrderResponseDto);
    }

    @Override
    @Transactional
    public OrderResponseDto processPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ValidationException("Only orders with PENDING status can be paid. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ValidationException("Only PAID orders can be shipped. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderStatus> cancellableStatuses = List.of(OrderStatus.PENDING, OrderStatus.PAID);

        if (!cancellableStatuses.contains(order.getStatus())) {
            throw new ValidationException("Cannot cancel order. Current status is: " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            Book book = bookRepository.findByIdForUpdate(item.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + item.getBookId()));

            book.setQuantity(book.getQuantity() + item.getQuantity());
            bookRepository.save(book);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    public OrderResponseDto getOrderDetails(Long orderId, UserDetails currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isOwner = order.getUserId().equals(currentUser.getUsername());

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return orderMapper.toOrderResponseDto(order);
    }
}