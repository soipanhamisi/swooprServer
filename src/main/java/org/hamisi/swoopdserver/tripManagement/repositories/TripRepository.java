package org.hamisi.swoopdserver.tripManagement.repositories;

import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    Trip getTripByCreatedBy(UUID userId);

    @Query("SELECT t FROM Trip t " +
                  "WHERE t.tripStatus = :tripStatus " +
                  "AND t.destinationZone = :destinationZone " +
                  "AND t.departureTime = :departure")
    List<Trip> getTripsByTripStatusDestinationZonedTime(TripStatus tripStatus, String destinationZone, LocalDateTime departure);


    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Trip t LEFT JOIN t.users u " +
            "WHERE t.tripStatus NOT IN (" +
            "org.hamisi.swoopdserver.tripManagement.entities.TripStatus.CANCELLED, " +
            "org.hamisi.swoopdserver.tripManagement.entities.TripStatus.COMPLETED) " +
            "AND (u.userId = :userId OR t.createdBy = :userId)")
    boolean belongsToAnOpenCarPool(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN t.users u " +
            "WHERE (u.userId = :userId OR t.createdBy = :userId) " +
            "AND t.tripStatus NOT IN (TripStatus.CANCELLED, TripStatus.COMPLETED)")
    Trip getOpenTripsWithUserId(@Param("userId") UUID userid);

    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN t.users u " +
            "WHERE (u.userId = :userId OR t.createdBy = :userId) " +
            "AND t.tripStatus <> TripStatus.OPEN")
    List<Trip> getAllNonOpenTripsByUserId(UUID userId);
}