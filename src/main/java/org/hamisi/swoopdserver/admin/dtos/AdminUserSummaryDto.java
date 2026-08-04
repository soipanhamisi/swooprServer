package org.hamisi.swoopdserver.admin.dtos;

import org.hamisi.swoopdserver.users.Role;

import java.util.UUID;

public record AdminUserSummaryDto(
        UUID userId,
        String fullName,
        String email,
        Role role,
        boolean hasMessagingToken
) {
}

