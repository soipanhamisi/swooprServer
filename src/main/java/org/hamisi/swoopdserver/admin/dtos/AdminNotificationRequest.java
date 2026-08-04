package org.hamisi.swoopdserver.admin.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class AdminNotificationRequest {
    private String title;
    private String message;
    private NotificationAudience audience = NotificationAudience.ALL_USERS;
    private List<UUID> selectedUserIds;
    private Set<NotificationChannel> channels;
}

