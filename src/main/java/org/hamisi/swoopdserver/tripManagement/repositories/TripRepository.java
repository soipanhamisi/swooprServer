package org.hamisi.swoopdserver.tripManagement.repositories;

import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    Trip getTripByCreatedBy(UUID userId);

    List<Trip> getTripsByTripStatus(TripStatus tripStatus);

    @Query("SELECT t FROM Trip t " +
                  "WHERE t.tripStatus = :tripStatus " +
                  "AND t.destinationZone = :destinationZone " +
                  "AND t.departureTime = :departure")
    List<Trip> getTripsByTripStatusDestinationZonedTime(TripStatus tripStatus, String destinationZone, LocalDateTime departure);

    @Query("SELECT u FROM Trip t JOIN t.users u WHERE t.tripId = :tripId")
    List<User> getTripUsersByTripId(UUID tripId);
}
