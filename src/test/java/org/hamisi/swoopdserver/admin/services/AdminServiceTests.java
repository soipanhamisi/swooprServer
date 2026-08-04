package org.hamisi.swoopdserver.admin.services;

import org.hamisi.swoopdserver.admin.dtos.AdminNotificationRequest;
import org.hamisi.swoopdserver.admin.dtos.AdminNotificationResultDto;
import org.hamisi.swoopdserver.admin.dtos.NotificationAudience;
import org.hamisi.swoopdserver.admin.dtos.NotificationChannel;
import org.hamisi.swoopdserver.admin.exceptions.AdminAccessDeniedException;
import org.hamisi.swoopdserver.admin.exceptions.AdminOperationException;
import org.hamisi.swoopdserver.auth.proxies.ResendProxy;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.repositories.RideSeekerBacklogRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.users.Role;
import org.hamisi.swoopdserver.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private FirebaseMessagingService firebaseMessagingService;

    @Mock
    private ResendProxy resendProxy;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("assertAdmin rejects non-admin users")
    void assertAdminRejectsNonAdminUsers() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "Student", "student@usiu.ac.ke", Role.NORMAL_USER, "token-1");
        when(usersRepository.getUserByUserId(userId)).thenReturn(user);

        assertThrows(AdminAccessDeniedException.class, () -> adminService.assertAdmin(userId));
    }

    @Test
    @DisplayName("demoteFromAdmin blocks self-demotion")
    void demoteFromAdminBlocksSelfDemotion() {
        UUID adminId = UUID.randomUUID();
        User admin = buildUser(adminId, "Admin", "admin@usiu.ac.ke", Role.ADMIN, "token-1");
        when(usersRepository.getUserByUserId(adminId)).thenReturn(admin);

        assertThrows(AdminOperationException.class, () -> adminService.demoteFromAdmin(adminId, adminId));
        verify(usersRepository, never()).updateRole(eq(adminId), eq(Role.NORMAL_USER));
    }

    @Test
    @DisplayName("sendAnnouncement targets selected users and splits Firebase and email delivery counts")
    void sendAnnouncementTargetsSelectedUsersAcrossChannels() {
        UUID adminId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        User admin = buildUser(adminId, "Admin", "admin@usiu.ac.ke", Role.ADMIN, "admin-token");
        User firstUser = buildUser(firstUserId, "Alice", "alice@usiu.ac.ke", Role.NORMAL_USER, "firebase-token");
        User secondUser = buildUser(secondUserId, "Bob", "bob@usiu.ac.ke", Role.NORMAL_USER, "   ");

        when(usersRepository.getUserByUserId(adminId)).thenReturn(admin);
        when(usersRepository.findByUserIdInOrderByFullNameAsc(anyCollection()))
                .thenReturn(List.of(firstUser, secondUser));

        AdminNotificationRequest request = new AdminNotificationRequest();
        request.setTitle("Service update");
        request.setMessage("Trips resume at 8 AM");
        request.setAudience(NotificationAudience.SELECTED_USERS);
        request.setSelectedUserIds(List.of(firstUserId, secondUserId));
        request.setChannels(Set.of(NotificationChannel.FCM, NotificationChannel.EMAIL));

        AdminNotificationResultDto result = adminService.sendAnnouncement(adminId, request);

        assertEquals(2, result.targetedUsers());
        assertEquals(1, result.fcmRecipients());
        assertEquals(2, result.emailRecipients());
        verify(firebaseMessagingService, times(1)).sendNotification(eq(firstUserId), eq("AdminService"), eq("ADMIN_ANNOUNCEMENT"), org.mockito.ArgumentMatchers.anyMap());
        verify(firebaseMessagingService, never()).sendNotification(eq(secondUserId), anyString(), anyString(), org.mockito.ArgumentMatchers.anyMap());
        verify(resendProxy, times(1)).sendEmail(eq("alice@usiu.ac.ke"), eq("Service update"), anyString(), anyString());
        verify(resendProxy, times(1)).sendEmail(eq("bob@usiu.ac.ke"), eq("Service update"), anyString(), anyString());
    }

    @Test
    @DisplayName("getUsers returns all users when no filter is requested")
    void getUsersReturnsAllUsers() {
        UUID adminId = UUID.randomUUID();
        User admin = buildUser(adminId, "Admin", "admin@usiu.ac.ke", Role.ADMIN, "admin-token");
        User firstUser = buildUser(UUID.randomUUID(), "Alice", "alice@usiu.ac.ke", Role.NORMAL_USER, "token-a");
        User secondUser = buildUser(UUID.randomUUID(), "Bob", "bob@usiu.ac.ke", Role.ADMIN, null);

        when(usersRepository.getUserByUserId(adminId)).thenReturn(admin);
        when(usersRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(firstUser, secondUser));

        var results = adminService.getUsers(adminId, false);

        assertEquals(2, results.size());
        assertEquals("Alice", results.getFirst().fullName());
        assertTrue(results.getFirst().hasMessagingToken());
        assertEquals(Role.ADMIN, results.get(1).role());
    }

    private User buildUser(UUID userId, String fullName, String email, Role role, String messagingToken) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setMessagingToken(messagingToken);
        return user;
    }
}


