package org.hamisi.swoopdserver.admin.dtos;

import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminTripSummaryDto(
        UUID tripId,
        TripStatus tripStatus,
        LocalDateTime departureTime,
        String originZone,
        String destinationZone,
        UUID createdBy,
        String hostName,
        String hostEmail,
        String vehicleRegNumber,
        int remainingCapacity,
        int participantCount,
        List<String> participantNames
) {
}

