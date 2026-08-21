package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.Vehicle;
import org.hamisi.swoopdserver.tripManagement.exceptions.RegisterVehicleException;
import org.hamisi.swoopdserver.tripManagement.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class VehicleManagementService {
    private final VehicleRepository vehicleRepository;
    private final UsersRepository usersRepository;

    public VehicleManagementService(VehicleRepository vehicleRepository, UsersRepository usersRepository) {
        this.vehicleRepository = vehicleRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public void registerVehicle(UUID userId, VehicleDto vehicleDto){
        if (!isValidRegNo(vehicleDto.getRegNo())){
            throw new RegisterVehicleException("invalid licence plate format");
        }
        String normalizedPlateNumber = normalizePlateNo(vehicleDto.getRegNo());
        if (vehicleRepository.existsByVehicleRegNumberContainingIgnoreCase(normalizedPlateNumber)){
            return;
        }
        vehicleRepository.save(
                new Vehicle()
                        .setVehicleDescription(vehicleDto.getDesc())
                        .setVehicleRegNumber(normalizedPlateNumber)
                        .setUser(usersRepository.getReferenceById(userId))
        );
    }
    @Transactional
    public List<VehicleDto> getRegisteredVehicles(UUID userId){
        List<Vehicle> vehicles = vehicleRepository.getAllByUser_UserId(userId);
        return parseVehiclesToVehicleDto(vehicles);
    }

    public boolean isVehicleOwnedByUser(UUID userId, String regNo){
        regNo = normalizePlateNo(regNo);
       List<Vehicle> vehicleList =  vehicleRepository.getAllByUser_UserId(userId);
       for (Vehicle vehicle: vehicleList){
           if (vehicle.getVehicleRegNumber().equals(regNo)){
               return true;
           }
       }
        return false;
    }

    private List<VehicleDto> parseVehiclesToVehicleDto(List<Vehicle> vehicles) {
        List<VehicleDto> vehicleDtos = new ArrayList<>();
        VehicleDto vehicleDto = new VehicleDto();
        for (Vehicle vehicle: vehicles){
            vehicleDto.setRegNo(vehicle.getVehicleRegNumber());
            vehicleDto.setDesc(vehicle.getVehicleDescription());
            vehicleDtos.add(vehicleDto);
        }
        return vehicleDtos;
    }

    private String normalizePlateNo(String regNo) {
        return regNo.trim().toUpperCase();
    }

    private boolean isValidRegNo(String regNo) {
        String plateRegex = "^K[A-HJ-NP-Z]{2}\\s?[0-9]{3}[A-HJ-NP-Z]$";
        return Pattern.matches(plateRegex, regNo.trim().toUpperCase());
    }
}
