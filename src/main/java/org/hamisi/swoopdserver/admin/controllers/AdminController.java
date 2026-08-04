package org.hamisi.swoopdserver.admin.controllers;

import org.hamisi.swoopdserver.admin.dtos.AdminBacklogEntryDto;
import org.hamisi.swoopdserver.admin.dtos.AdminNotificationRequest;
import org.hamisi.swoopdserver.admin.dtos.AdminNotificationResultDto;
import org.hamisi.swoopdserver.admin.dtos.AdminTripSummaryDto;
import org.hamisi.swoopdserver.admin.dtos.AdminUserSummaryDto;
import org.hamisi.swoopdserver.admin.services.AdminService;
import org.hamisi.swoopdserver.auth.dtos.LoginCredentials;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final TokenManagementService tokenManagementService;

    public AdminController(AdminService adminService, TokenManagementService tokenManagementService) {
        this.adminService = adminService;
        this.tokenManagementService = tokenManagementService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginCredentials credentials) {
        String token = adminService.adminLogin(credentials.getEmail(), credentials.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Login successful", token));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserSummaryDto>>> getUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "false") boolean fcmOnly
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", adminService.getUsers(adminUserId, fcmOnly)));
    }

    @PostMapping("/users/{userId}/promote")
    public ResponseEntity<ApiResponse<Void>> promoteUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID userId
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        adminService.promoteToAdmin(adminUserId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("User promoted to admin"));
    }

    @PostMapping("/users/{userId}/demote")
    public ResponseEntity<ApiResponse<Void>> demoteUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID userId
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        adminService.demoteFromAdmin(adminUserId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("User demoted from admin"));
    }

    @GetMapping("/backlog")
    public ResponseEntity<ApiResponse<List<AdminBacklogEntryDto>>> getBacklog(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "false") boolean includeMatched
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Backlog retrieved", adminService.getBacklog(adminUserId, includeMatched)));
    }

    @GetMapping("/trips/active")
    public ResponseEntity<ApiResponse<List<AdminTripSummaryDto>>> getActiveTrips(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Active trips retrieved", adminService.getActiveTrips(adminUserId)));
    }

    @GetMapping("/trips/non-open")
    public ResponseEntity<ApiResponse<List<AdminTripSummaryDto>>> getNonOpenTrips(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Non-open trips retrieved", adminService.getNonOpenTrips(adminUserId)));
    }

    @PostMapping("/notifications")
    public ResponseEntity<ApiResponse<AdminNotificationResultDto>> sendAnnouncement(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AdminNotificationRequest request
    ) {
        UUID adminUserId = requireAdminUserId(authHeader);
        AdminNotificationResultDto result = adminService.sendAnnouncement(adminUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Announcement sent", result));
    }

    private UUID requireAdminUserId(String authHeader) {
        UUID adminUserId = tokenManagementService.verifyToken(authHeader).getUserId();
        adminService.assertAdmin(adminUserId);
        return adminUserId;
    }
}

