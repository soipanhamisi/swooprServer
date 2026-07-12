package org.hamisi.swoopdserver.tripManagement.repositories;

import org.hamisi.swoopdserver.tripManagement.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Vehicle findVehicleByUser_UserId(UUID userUserId);

    List<Vehicle> getAllByUser_UserId(UUID userId);
}
