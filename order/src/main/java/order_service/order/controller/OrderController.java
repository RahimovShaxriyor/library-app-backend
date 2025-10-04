package order_service.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.order.dto.CreateOrderRequestDto;
import order_service.order.dto.OrderResponseDto;
import order_service.order.model.OrderStatus;
import order_service.order.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing book orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Create a new order", description = "Create order with book items. Minimum order amount: 50,000")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto createOrder(
            @Valid @RequestBody CreateOrderRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("📦 Creating order for user: {}", currentUser.getUsername());
        request.logOrderDetails();
        return orderService.createOrder(request, currentUser);
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Get my order history", description = "Get paginated order history for current user")
    public Page<OrderResponseDto> getMyOrderHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable) {

        log.debug("📋 Fetching order history for user: {}", currentUser.getUsername());
        return orderService.getOrdersForUser(currentUser, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @Operation(summary = "Get order by ID", description = "Get order details. Users can only see their own orders")
    public OrderResponseDto getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("🔍 Fetching order {} for user: {}", id, currentUser.getUsername());
        return orderService.getOrderDetails(id, currentUser);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Process order payment", description = "Process payment for a pending order")
    public OrderResponseDto processPayment(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("💳 Processing payment for order {} by user: {}", id, currentUser.getUsername());
        return orderService.processPayment(id, currentUser);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @Operation(summary = "Cancel order", description = "Cancel an order. Users can only cancel their own orders")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("❌ Cancelling order {} by user: {}", id, currentUser.getUsername());
        orderService.cancelOrder(id, currentUser);
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get all orders", description = "Get all orders (Admin only)")
    public Page<OrderResponseDto> getAllOrders(
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        log.debug("👨‍💼 Admin fetching all orders");
        return orderService.getAllOrders(pageable);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get orders by status", description = "Get orders by status (Admin only)")
    public Page<OrderResponseDto> getOrdersByStatus(
            @Parameter(description = "Order status") @PathVariable OrderStatus status,
            @Parameter(description = "Pagination parameters") Pageable pageable) {

        log.debug("👨‍💼 Admin fetching orders by status: {}", status);
        return orderService.getOrdersByStatus(status, pageable);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Ship order", description = "Mark order as shipped (Admin only)")
    public OrderResponseDto shipOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {

        log.info("🚚 Shipping order: {}", id);
        return orderService.shipOrder(id);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Deliver order", description = "Mark order as delivered (Admin only)")
    public OrderResponseDto deliverOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {

        log.info("📮 Delivering order: {}", id);
        return orderService.deliverOrder(id);
    }
}