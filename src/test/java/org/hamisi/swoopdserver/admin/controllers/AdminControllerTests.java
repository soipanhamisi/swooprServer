package org.hamisi.swoopdserver.admin.controllers;

import org.hamisi.swoopdserver.admin.dtos.AdminNotificationResultDto;
import org.hamisi.swoopdserver.admin.dtos.AdminUserSummaryDto;
import org.hamisi.swoopdserver.admin.exceptions.AdminAccessDeniedException;
import org.hamisi.swoopdserver.admin.services.AdminService;
import org.hamisi.swoopdserver.common.AccessRecord;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTests {

    private static final String AUTH_HEADER = "Bearer admin-token";

    private AdminService adminService;
    private TokenManagementService tokenManagementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        tokenManagementService = mock(TokenManagementService.class);

        AdminController controller = new AdminController(adminService, tokenManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AdminControllerExceptionHandlers())
                .build();
    }

    @Test
    @DisplayName("GET /admin/users returns the user list")
    void getUsersReturnsUserList() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(tokenManagementService.verifyToken(AUTH_HEADER))
                .thenReturn(new AccessRecord(adminId.toString(), "admin@usiu.ac.ke"));
        doNothing().when(adminService).assertAdmin(adminId);
        when(adminService.getUsers(adminId, false))
                .thenReturn(List.of(new AdminUserSummaryDto(userId, "Alice", "alice@usiu.ac.ke", Role.NORMAL_USER, true)));

        mockMvc.perform(get("/admin/users").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users retrieved"))
                .andExpect(jsonPath("$.data[0].email").value("alice@usiu.ac.ke"))
                .andExpect(jsonPath("$.data[0].hasMessagingToken").value(true));

        verify(adminService).getUsers(adminId, false);
    }

    @Test
    @DisplayName("POST /admin/notifications returns a delivery summary")
    void sendAnnouncementReturnsDeliverySummary() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(tokenManagementService.verifyToken(AUTH_HEADER))
                .thenReturn(new AccessRecord(adminId.toString(), "admin@usiu.ac.ke"));
        doNothing().when(adminService).assertAdmin(adminId);
        when(adminService.sendAnnouncement(eq(adminId), any()))
                .thenReturn(new AdminNotificationResultDto(5, 3, 5));

        mockMvc.perform(post("/admin/notifications")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Maintenance",
                                  "message": "Trips will pause at midnight",
                                  "audience": "ALL_USERS",
                                  "channels": ["FCM", "EMAIL"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Announcement sent"))
                .andExpect(jsonPath("$.data.targetedUsers").value(5))
                .andExpect(jsonPath("$.data.fcmRecipients").value(3))
                .andExpect(jsonPath("$.data.emailRecipients").value(5));
    }

    @Test
    @DisplayName("Non-admin requests are rejected with 403")
    void nonAdminRequestsAreRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        when(tokenManagementService.verifyToken(AUTH_HEADER))
                .thenReturn(new AccessRecord(userId.toString(), "student@usiu.ac.ke"));
        org.mockito.Mockito.doThrow(new AdminAccessDeniedException("Admin privileges are required for this action"))
                .when(adminService).assertAdmin(userId);

        mockMvc.perform(get("/admin/users").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Admin privileges are required for this action"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}

