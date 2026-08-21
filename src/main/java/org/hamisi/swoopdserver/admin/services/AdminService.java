package org.hamisi.swoopdserver.admin.services;

import org.hamisi.swoopdserver.admin.dtos.AdminBacklogEntryDto;
import org.hamisi.swoopdserver.admin.dtos.AdminNotificationRequest;
import org.hamisi.swoopdserver.admin.dtos.AdminNotificationResultDto;
import org.hamisi.swoopdserver.admin.dtos.AdminTripSummaryDto;
import org.hamisi.swoopdserver.admin.dtos.AdminUserSummaryDto;
import org.hamisi.swoopdserver.admin.dtos.NotificationAudience;
import org.hamisi.swoopdserver.admin.dtos.NotificationChannel;
import org.hamisi.swoopdserver.admin.exceptions.AdminAccessDeniedException;
import org.hamisi.swoopdserver.admin.exceptions.AdminLoginException;
import org.hamisi.swoopdserver.admin.exceptions.AdminOperationException;
import org.hamisi.swoopdserver.admin.exceptions.AdminResourceNotFoundException;
import org.hamisi.swoopdserver.auth.proxies.ResendProxy;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.auth.services.HashingService;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.repositories.RideSeekerBacklogRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.users.Role;
import org.hamisi.swoopdserver.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminService {
    private static final String ADMIN_NOTIFICATION_SOURCE = "AdminService";
    private static final String ADMIN_NOTIFICATION_TYPE = "ADMIN_ANNOUNCEMENT";

    private final UsersRepository usersRepository;
    private final RideSeekerBacklogRepository rideSeekerBacklogRepository;
    private final TripRepository tripRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final ResendProxy resendProxy;
    private final HashingService hashingService;
    private final TokenManagementService tokenManagementService;

    public AdminService(UsersRepository usersRepository,
                        RideSeekerBacklogRepository rideSeekerBacklogRepository,
                        TripRepository tripRepository,
                        FirebaseMessagingService firebaseMessagingService,
                        ResendProxy resendProxy,
                        HashingService hashingService,
                        TokenManagementService tokenManagementService) {
        this.usersRepository = usersRepository;
        this.rideSeekerBacklogRepository = rideSeekerBacklogRepository;
        this.tripRepository = tripRepository;
        this.firebaseMessagingService = firebaseMessagingService;
        this.resendProxy = resendProxy;
        this.hashingService = hashingService;
        this.tokenManagementService = tokenManagementService;
    }

    public void assertAdmin(UUID adminUserId) {
        User admin = usersRepository.getUserByUserId(adminUserId);
        if (admin == null) {
            throw new AdminResourceNotFoundException("Admin user was not found");
        }
        if (admin.getRole() != Role.ADMIN) {
            throw new AdminAccessDeniedException("Admin privileges are required for this action");
        }
    }

    public String adminLogin(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AdminLoginException("Invalid credentials");
        }
        User user = usersRepository.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            throw new AdminLoginException("Invalid credentials");
        }
        String hashedInput = hashingService.hashPassword(password);
        if (!hashedInput.equals(user.getPassword())) {
            throw new AdminLoginException("Invalid credentials");
        }
        if (user.getRole() != Role.ADMIN) {
            throw new AdminLoginException("This account does not have admin privileges");
        }
        return tokenManagementService.createToken(user.getUserId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryDto> getUsers(UUID adminUserId, boolean fcmOnly) {
        assertAdmin(adminUserId);
        List<User> users = fcmOnly
                ? usersRepository.findUsersWithMessagingTokens()
                : usersRepository.findAllByOrderByFullNameAsc();
        return users.stream().map(this::toUserSummary).toList();
    }

    @Transactional
    public void promoteToAdmin(UUID adminUserId, UUID targetUserId) {
        assertAdmin(adminUserId);
        getRequiredUser(targetUserId);
        usersRepository.updateRole(targetUserId, Role.ADMIN);
    }

    @Transactional
    public void demoteFromAdmin(UUID adminUserId, UUID targetUserId) {
        assertAdmin(adminUserId);
        if (adminUserId.equals(targetUserId)) {
            throw new AdminOperationException("You cannot demote your own admin account");
        }
        User targetUser = getRequiredUser(targetUserId);
        if (targetUser.getRole() != Role.ADMIN) {
            return;
        }
        usersRepository.updateRole(targetUserId, Role.NORMAL_USER);
    }

    @Transactional(readOnly = true)
    public List<AdminBacklogEntryDto> getBacklog(UUID adminUserId, boolean includeMatched) {
        assertAdmin(adminUserId);
        List<RideSeekerBacklogEntry> entries = includeMatched
                ? rideSeekerBacklogRepository.findAllEntriesWithUsersOrderByRequestMadeAtAsc()
                : rideSeekerBacklogRepository.findUnmatchedEntriesWithUsersOrderByRequestMadeAtAsc();
        return entries.stream().map(this::toBacklogEntry).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminTripSummaryDto> getActiveTrips(UUID adminUserId) {
        assertAdmin(adminUserId);
        return tripRepository.findActiveTripsForAdmin().stream().map(this::toTripSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminTripSummaryDto> getNonOpenTrips(UUID adminUserId) {
        assertAdmin(adminUserId);
        return tripRepository.findNonOpenTripsForAdmin().stream().map(this::toTripSummary).toList();
    }

    @Transactional
    public AdminNotificationResultDto sendAnnouncement(UUID adminUserId, AdminNotificationRequest request) {
        assertAdmin(adminUserId);
        validateNotificationRequest(request);

        List<User> targetUsers = resolveAudience(request.getAudience(), request.getSelectedUserIds());
        int fcmRecipients = 0;
        int emailRecipients = 0;

        Set<NotificationChannel> channels = request.getChannels();
        if (channels.contains(NotificationChannel.FCM)) {
            for (User user : targetUsers) {
                if (!hasMessagingToken(user)) {
                    continue;
                }
                firebaseMessagingService.sendNotification(
                        user.getUserId(),
                        ADMIN_NOTIFICATION_SOURCE,
                        ADMIN_NOTIFICATION_TYPE,
                        Map.of(
                                "title", request.getTitle().trim(),
                                "message", request.getMessage().trim()
                        )
                );
                fcmRecipients++;
            }
        }

        if (channels.contains(NotificationChannel.EMAIL)) {
            String subject = request.getTitle().trim();
            String plainText = buildPlainTextAnnouncement(request.getTitle(), request.getMessage());
            String html = buildHtmlAnnouncement(request.getTitle(), request.getMessage());
            for (User user : targetUsers) {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    continue;
                }
                resendProxy.sendEmail(user.getEmail(), subject, html, plainText);
                emailRecipients++;
            }
        }

        return new AdminNotificationResultDto(targetUsers.size(), fcmRecipients, emailRecipients);
    }

    private void validateNotificationRequest(AdminNotificationRequest request) {
        if (request == null) {
            throw new AdminOperationException("Notification payload is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new AdminOperationException("Notification title is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new AdminOperationException("Notification message is required");
        }
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new AdminOperationException("At least one notification channel must be selected");
        }
        if (request.getAudience() == NotificationAudience.SELECTED_USERS
                && (request.getSelectedUserIds() == null || request.getSelectedUserIds().isEmpty())) {
            throw new AdminOperationException("Select at least one user when using the selected users audience");
        }
    }

    private List<User> resolveAudience(NotificationAudience audience, List<UUID> selectedUserIds) {
        NotificationAudience resolvedAudience = audience == null ? NotificationAudience.ALL_USERS : audience;
        if (resolvedAudience == NotificationAudience.ALL_USERS) {
            return usersRepository.findAllByOrderByFullNameAsc();
        }

        LinkedHashSet<UUID> uniqueUserIds = new LinkedHashSet<>(selectedUserIds);
        List<User> selectedUsers = usersRepository.findByUserIdInOrderByFullNameAsc(uniqueUserIds);
        if (selectedUsers.isEmpty()) {
            throw new AdminResourceNotFoundException("No matching users were found for the selected audience");
        }
        if (selectedUsers.size() != uniqueUserIds.size()) {
            throw new AdminOperationException("One or more selected users could not be found");
        }
        return selectedUsers;
    }

    private User getRequiredUser(UUID userId) {
        User user = usersRepository.getUserByUserId(userId);
        if (user == null) {
            throw new AdminResourceNotFoundException("User not found");
        }
        return user;
    }

    private AdminUserSummaryDto toUserSummary(User user) {
        return new AdminUserSummaryDto(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                hasMessagingToken(user)
        );
    }

    private AdminBacklogEntryDto toBacklogEntry(RideSeekerBacklogEntry entry) {
        User user = entry.getUser();
        return new AdminBacklogEntryDto(
                entry.getBacklogEntryId(),
                user == null ? null : user.getUserId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                entry.getRequestMadeAt(),
                entry.getSelectedDepartureTime(),
                entry.isMatched(),
                entry.getMatchedAt()
        );
    }

    private AdminTripSummaryDto toTripSummary(Trip trip) {
        List<String> participantNames = trip.getUsers() == null
                ? List.of()
                : trip.getUsers().stream()
                .filter(user -> user != null && user.getFullName() != null)
                .map(User::getFullName)
                .toList();

        User host = trip.getVehicle() == null ? null : trip.getVehicle().getUser();
        return new AdminTripSummaryDto(
                trip.getTripId(),
                trip.getTripStatus(),
                trip.getDepartureTime(),
                trip.getOriginZone(),
                trip.getDestinationZone(),
                trip.getCreatedBy(),
                host == null ? null : host.getFullName(),
                host == null ? null : host.getEmail(),
                trip.getVehicle() == null ? null : trip.getVehicle().getVehicleRegNumber(),
                trip.getTripCapacity(),
                participantNames.size(),
                participantNames
        );
    }

    private boolean hasMessagingToken(User user) {
        return user != null
                && user.getMessagingToken() != null
                && !user.getMessagingToken().isBlank();
    }

    private String buildPlainTextAnnouncement(String title, String message) {
        return title.trim() + System.lineSeparator() + System.lineSeparator() + message.trim();
    }

    private String buildHtmlAnnouncement(String title, String message) {
        String escapedTitle = escapeHtml(title.trim());
        String escapedMessage = escapeHtml(message.trim()).replace("\n", "<br />");
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <div style="max-width: 640px; margin: 0 auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 16px;">
                      <p style="margin: 0 0 8px; font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.08em;">Swoopd Admin Announcement</p>
                      <h2 style="margin: 0 0 16px;">%s</h2>
                      <p style="margin: 0; line-height: 1.7;">%s</p>
                    </div>
                  </body>
                </html>
                """.formatted(escapedTitle, escapedMessage);
    }

    private String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}


