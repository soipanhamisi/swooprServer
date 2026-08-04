package org.hamisi.swoopdserver.admin.dtos;

public record AdminNotificationResultDto(
        int targetedUsers,
        int fcmRecipients,
        int emailRecipients
) {
}

