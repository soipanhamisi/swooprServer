package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.AccessRecord;
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
    @PostMapping("regidterVehicle")
    public ResponseEntity<String> registerVehicle(@RequestHeader String jwt, @RequestBody VehicleDto vehicleDto){
        UUID userId = tokenManagementService.verifyToken(jwt).getUserId();
        tripManagementService.registerVehicle(userId, vehicleDto);
        return ResponseEntity.status(HttpStatus.OK).body("Registered vehicle successfully");
    }

    @PostMapping("createTrip")
    public ResponseEntity<String> createTrip(
            @RequestHeader String jwt,
            @RequestBody TripData createTripDto
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(jwt);
        tripManagementService.createTrip(accessRecord.getUserId(),
                createTripDto.getCapacity(),
                createTripDto.getDepartureTime(),
                createTripDto.getOriginDestinationCoordinates());
        return ResponseEntity.status(HttpStatus.CREATED).body("Trip Created Successfully");
    }

    @PostMapping("cancelTrip")
    public ResponseEntity<String> cancelTrip(@RequestHeader String jwt){
        AccessRecord accessRecord = tokenManagementService.verifyToken(jwt);
        tripManagementService.cancelTrip(accessRecord.getUserId());
        return ResponseEntity.status(HttpStatus.OK).body("Trip Cancelled Successfully");
    }

    @PostMapping("joinCarPool")
    public ResponseEntity<Trip> joinCarPool(@RequestHeader String jwt, @RequestBody JoinCarpoolDto joinCarpoolDto){
        AccessRecord accessRecord = tokenManagementService.verifyToken(jwt);
        UUID useerId = accessRecord.getUserId();
        Trip trip = tripManagementService.joinCarpool(useerId,
                joinCarpoolDto.getDepartureTime(),
                joinCarpoolDto.getRsOriginDestination());
        logger.info("inbound coordinates: {}", formatCoordinates(joinCarpoolDto.getRsOriginDestination()));
        return ResponseEntity.status(HttpStatus.OK).body(trip);
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
