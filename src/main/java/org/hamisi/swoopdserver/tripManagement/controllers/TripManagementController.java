package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.tripManagement.dtos.JoinCarpoolDto;
import org.hamisi.swoopdserver.tripManagement.dtos.RideRequest;
import org.hamisi.swoopdserver.tripManagement.dtos.TripCreationDTO;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.services.CarpoolMatchingService;
import org.hamisi.swoopdserver.tripManagement.services.TripLifecycleManagementService;
import org.hamisi.swoopdserver.tripManagement.services.VehicleManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.HttpResource;

import java.net.http.HttpResponse;
import java.util.UUID;

@RestController
@RequestMapping("/trip-management")
public class TripManagementController {
    private final TokenManagementService tokenManagementService;
    private final TripLifecycleManagementService tripLifecycleManagementService;
    private final CarpoolMatchingService carpoolMatchingService;
    private final VehicleManagementService vehicleManagementService;

    public TripManagementController(TokenManagementService tokenManagementService, TripLifecycleManagementService tripLifecycleManagementService, CarpoolMatchingService carpoolMatchingService, VehicleManagementService vehicleManagementService) {
        this.tokenManagementService = tokenManagementService;
        this.tripLifecycleManagementService = tripLifecycleManagementService;
        this.carpoolMatchingService = carpoolMatchingService;
        this.vehicleManagementService = vehicleManagementService;
    }

    @PostMapping("/postTrip")
    public ResponseEntity<ApiResponse<Void>> postTrip(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TripCreationDTO tripCreationDTO
            ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        tripLifecycleManagementService.createTrip(
                userId,
                tripCreationDTO.getTripCapacity(),
                tripCreationDTO.getDepartureTime(),
                tripCreationDTO.getVehicleDto(),
                tripCreationDTO.getOriginDestination()
        );
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Create trip request submitted successfully"));
    }

    @PostMapping("/postRideRequest")
    public ResponseEntity<ApiResponse<Void>> postRideRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JoinCarpoolDto joinCarpoolDto
            ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        carpoolMatchingService.matchRiderOrBacklog(
                userId,
                joinCarpoolDto.getDepartureTime(),
                joinCarpoolDto.getRsOriginDestination()
        );
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Ride request submitted successfully"));
    }

    @PostMapping("/postVehicle")
    public ResponseEntity<ApiResponse<Void>> postVehicle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody VehicleDto vehicleDto
            ){
        UUID useId = tokenManagementService.verifyToken(authHeader).getUserId();
        vehicleManagementService.registerVehicle(useId, vehicleDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Vehicle saved successfully"));
    }
}
