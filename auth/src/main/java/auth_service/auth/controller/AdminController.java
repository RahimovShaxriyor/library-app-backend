package auth_service.auth.controller;

import auth_service.auth.dto.ApiResponse;
import auth_service.auth.dto.UpdateUserStatusRequest;
import auth_service.auth.dto.UserAdminSummaryDto;
import auth_service.auth.dto.UserAdminViewDto;
import auth_service.auth.entity.Role;
import auth_service.auth.entity.User;
import auth_service.auth.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;


    @GetMapping("/users")
    public ResponseEntity<Page<UserAdminSummaryDto>> getAllUsers(
            @PageableDefault(sort = "createdAt,desc") Pageable pageable,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Set<Role> roles) {

        Page<UserAdminSummaryDto> users = adminService.getAllUsers(pageable, email, roles);
        return ResponseEntity.ok(users);
    }


    @GetMapping("/users/{userId}")
    public ResponseEntity<UserAdminViewDto> getUserById(@PathVariable UUID userId) {
        UserAdminViewDto user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }


    @PostMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal User adminUser) {
        adminService.updateUserStatus(userId, request, adminUser);
        return ResponseEntity.ok(new ApiResponse(true, "Статус пользователя успешно обновлен."));
    }
}

