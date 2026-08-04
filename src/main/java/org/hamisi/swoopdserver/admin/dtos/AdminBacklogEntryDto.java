package org.hamisi.swoopdserver.admin.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminBacklogEntryDto(
        UUID backlogEntryId,
        UUID userId,
        String fullName,
        String email,
        String originZone,
        String destinationZone,
        LocalDateTime requestMadeAt,
        LocalDateTime selectedDepartureTime,
        boolean matched,
        LocalDateTime matchedAt
) {
}

