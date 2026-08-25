package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.tripManagement.dtos.JoinCarpoolDto;
import org.hamisi.swoopdserver.tripManagement.dtos.TripCreationDTO;
import org.hamisi.swoopdserver.tripManagement.dtos.TripData;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.hamisi.swoopdserver.tripManagement.services.BacklogManagementService;
import org.hamisi.swoopdserver.tripManagement.services.CarpoolMatchingService;
import org.hamisi.swoopdserver.tripManagement.services.TripLifecycleManagementService;
import org.hamisi.swoopdserver.tripManagement.services.VehicleManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trip-management")
public class TripManagementController {
    private final TokenManagementService tokenManagementService;
    private final TripLifecycleManagementService tripLifecycleManagementService;
    private final CarpoolMatchingService carpoolMatchingService;
    private final VehicleManagementService vehicleManagementService;
    private final BacklogManagementService backlogManagementService;

    public TripManagementController(TokenManagementService tokenManagementService,
                                    TripLifecycleManagementService tripLifecycleManagementService,
                                    CarpoolMatchingService carpoolMatchingService,
                                    VehicleManagementService vehicleManagementService,
                                    BacklogManagementService backlogManagementService) {
        this.tokenManagementService = tokenManagementService;
        this.tripLifecycleManagementService = tripLifecycleManagementService;
        this.carpoolMatchingService = carpoolMatchingService;
        this.vehicleManagementService = vehicleManagementService;
        this.backlogManagementService = backlogManagementService;
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

    @PostMapping("/removeVehicle")
    public ResponseEntity<ApiResponse<Void>> removeVehicle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody VehicleDto vehicleDto
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        vehicleManagementService.deleteVehicle(userId, vehicleDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation success"));
    }

    @PostMapping("/cancelCarpool")
    public ResponseEntity<ApiResponse<Void>> cancelCarpool(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody String tripId
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        tripLifecycleManagementService.cancelTrip(userId, UUID.fromString(tripId));
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Cancel trip request submitted successfully"));
    }

    @PostMapping("/cancelRideRequest")
    public ResponseEntity<ApiResponse<Void>> cancelRideRequest(
            @RequestHeader("Authorization") String authHeader
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        backlogManagementService.cancelBacklogRequest(userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Successful"));
    }

    @PostMapping("/leaveCarpool")
    public ResponseEntity<ApiResponse<Void>> leaveCarpool(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody String tripId
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        tripLifecycleManagementService.leaveCarpool(userId, UUID.fromString(tripId));
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Successful"));
    }

    @GetMapping("/getTripInfo")
    public ResponseEntity<ApiResponse<TripData>> getTripInfo(
            @RequestHeader String authHeader,
            @RequestBody String tripId
    ){
        tokenManagementService.verifyToken(authHeader);
        TripData tripInfo = tripLifecycleManagementService.getTripInfo(UUID.fromString(tripId));
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Success", tripInfo));
    }

    @GetMapping("/getRideRequestInfoById")
    public ResponseEntity<ApiResponse<RideSeekerBacklogEntry>> getRideRequestInfo(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody String backlogId
    ){
        tokenManagementService.verifyToken(authHeader);
        RideSeekerBacklogEntry rideSeekerBacklogEntry = backlogManagementService.getRideRequest(backlogId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Success", rideSeekerBacklogEntry));
    }

    @GetMapping("/getActiveRideRequests")
    public ResponseEntity<ApiResponse<RideSeekerBacklogEntry>> getRideRequestInfo(
            @RequestHeader("Authorization") String authHeader
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Success", backlogManagementService.getActiveBacklogRequest(userId)));
    }

    @GetMapping("/getRegisteredVehicles")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> getRegisteredVehicles(
            @RequestHeader("Authorization") String authHeader
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Success", vehicleManagementService.getRegisteredVehicles(userId)));
    }
}
