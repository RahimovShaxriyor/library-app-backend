package order_service.order.controller;

import io.swagger.v3.oas.annotations.Parameter;
import order_service.order.dto.CreateOrderRequestDto;
import order_service.order.dto.OrderResponseDto;
import order_service.order.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody CreateOrderRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        OrderResponseDto createdOrder = orderService.createOrder(request, currentUser);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }


    @GetMapping("/my-history")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<Page<OrderResponseDto>> getMyOrderHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser,
            Pageable pageable) {

        Page<OrderResponseDto> orderHistory = orderService.getOrdersForUser(currentUser, pageable);
        return ResponseEntity.ok(orderHistory);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        OrderResponseDto orderDetails = orderService.getOrderDetails(id, currentUser);
        return ResponseEntity.ok(orderDetails);
    }
}