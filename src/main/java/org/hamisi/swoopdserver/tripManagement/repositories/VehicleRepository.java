package org.hamisi.swoopdserver.tripManagement.repositories;

import org.hamisi.swoopdserver.tripManagement.entities.Vehicle;
import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findVehicleByUser_UserId(UUID userUserId);

    List<Vehicle> getAllByUser_UserId(UUID userId);

    boolean existsByVehicleRegNumber(String regNo);

    boolean existsByVehicleRegNumberContainingIgnoreCase(String normalizedPlateNumber);

    Vehicle findByUserAndVehicleRegNumber(User referenceById, String regNo);
}
