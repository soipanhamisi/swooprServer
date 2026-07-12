package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.AccessRecord;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.tripManagement.dtos.JoinCarpoolDto;
import org.hamisi.swoopdserver.tripManagement.dtos.TripData;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.services.TripManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripManagementController {

    private final TripManagementService tripManagementService;
    private final TokenManagementService tokenManagementService;
    private static final Logger logger = LoggerFactory.getLogger(TripManagementController.class);

    public TripManagementController(TripManagementService tripManagementService, TokenManagementService tokenManagementService) {
        this.tripManagementService = tripManagementService;
        this.tokenManagementService = tokenManagementService;
    }
    @PostMapping("registerVehicle")
    public ResponseEntity<String> registerVehicle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody VehicleDto vehicleDto
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        tripManagementService.registerVehicle(userId, vehicleDto);
        return ResponseEntity.status(HttpStatus.OK).body("Registered vehicle successfully");
    }

    @PostMapping("queryRegisteredVehicle")
    public ResponseEntity<List<VehicleDto>> queryRegisteredVehicles(
            @RequestHeader("Authorization") String authHeader
    ){
      UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
      List<VehicleDto> vehicleList = tripManagementService.getRegisteredVehicles(userId);
      return ResponseEntity.status(HttpStatus.OK).body(vehicleList);
    }

    @PostMapping("createTrip")
    public ResponseEntity<String> createTrip(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TripData createTripDto
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        tripManagementService.createTrip(accessRecord.getUserId(),
                createTripDto.getCapacity(),
                createTripDto.getDepartureTime(),
                createTripDto.getOriginDestinationCoordinates());
        return ResponseEntity.status(HttpStatus.CREATED).body("Trip Created Successfully");
    }

    @PostMapping("cancelTrip")
    public ResponseEntity<String> cancelTrip(@RequestHeader("Authorization") String authHeader){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        tripManagementService.cancelTrip(accessRecord.getUserId());
        return ResponseEntity.status(HttpStatus.OK).body("Trip Cancelled Successfully");
    }

    @PostMapping( "joinCarpool")
    public ResponseEntity<ApiResponse<Trip>> joinCarPool(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JoinCarpoolDto joinCarpoolDto
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        UUID useerId = accessRecord.getUserId();
        Trip trip = tripManagementService.joinCarpool(useerId,
                joinCarpoolDto.getDepartureTime(),
                joinCarpoolDto.getRsOriginDestination());
        logger.info("inbound coordinates: {}", formatCoordinates(joinCarpoolDto.getRsOriginDestination()));
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Carpool joined successfully", trip));
    }

    private String formatCoordinates(Object coordinates) {
        if (coordinates == null) {
            return "null";
        }

        if (!coordinates.getClass().isArray()) {
            return coordinates.toString();
        }

        int length = Array.getLength(coordinates);
        StringBuilder formatted = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                formatted.append(", ");
            }
            formatted.append(Array.get(coordinates, i));
        }
        formatted.append(']');
        return formatted.toString();
    }

}
