package org.hamisi.swoopdserver.tripManagement.repositories;

import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    @Modifying
    @Query("UPDATE Trip t SET t.tripStatus = 'CANCELLED' WHERE EXISTS (SELECT 1 FROM t.users u WHERE u.email = ?)")
    void cancelTrip(String email);
}
