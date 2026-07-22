package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.AccessRecord;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.tripManagement.dtos.*;
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

/**
 * Trip management endpoints for vehicle registration, trip lifecycle, and carpool joins.
 *
 * <p>Most responses use the shared {@code ApiResponse} envelope:</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": null
 * }
 * }</pre>
 */
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

    /**
     * Registers the authenticated user's vehicle.
     *
     * <p>Inbound JSON ({@link VehicleDto}):</p>
     * <pre>{@code
     * {
     *   "regNo": "KAA 123A",
     *   "desc": "Silver Toyota Noah"
     * }
     * }</pre>
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Vehicle registered",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("registerVehicle")
    public ResponseEntity<ApiResponse<Void>> registerVehicle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody VehicleDto vehicleDto
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        tripManagementService.registerVehicle(userId, vehicleDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Vehicle registered"));
    }

    /**
     * Returns the vehicles already registered by the authenticated user.
     *
     * <p>Inbound:</p>
     * <ul>
     *   <li>{@code Authorization} header with a bearer token</li>
     * </ul>
     *
     * <p>Outbound JSON is a bare array of {@link VehicleDto} objects, not wrapped in {@code ApiResponse}:</p>
     * <pre>{@code
     * [
     *   {
     *     "regNo": "KAA 123A",
     *     "desc": "Silver Toyota Noah"
     *   }
     * ]
     * }</pre>
     */
    @PostMapping("queryRegisteredVehicle")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> queryRegisteredVehicles(
            @RequestHeader("Authorization") String authHeader
    ){
      UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
      List<VehicleDto> vehicleList = tripManagementService.getRegisteredVehicles(userId);
      return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(vehicleList));
    }

    /**
     * Creates a trip for the authenticated host.
     *
     * <p>Inbound JSON ({@link TripData}):</p>
     * <pre>{@code
     * {
     *   "capacity": 4,
     *   "departureTime": "2026-07-13T08:00:00",
     *   "originDestinationCoordinates": {
     *     "originLongitude": 36.807,
     *     "originLatitude": -1.283,
     *     "destinationLongitude": 36.812,
     *     "destinationLatitude": -1.300
     *   }
     * }
     * }</pre>
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Trip Created",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("createTrip")
    public ResponseEntity<ApiResponse<Void>> createTrip(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TripData createTripDto
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        tripManagementService.createTrip(accessRecord.getUserId(),
                createTripDto.getCapacity(),
                createTripDto.getDepartureTime(),
                createTripDto.getOriginDestinationCoordinates());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Trip Created"));
    }

    /**
     * Cancels the authenticated user's active trip.
     *
     * <p>Inbound:</p>
     * <ul>
     *   <li>{@code Authorization} header with a bearer token</li>
     * </ul>
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Trip Cancelled",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("cancelTrip")
    public ResponseEntity<ApiResponse<Void>> cancelTrip(@RequestHeader("Authorization") String authHeader){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        tripManagementService.cancelTrip(accessRecord.getUserId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Trip Cancelled"));
    }

    /**
     * Joins the best matching carpool for the authenticated ride-seeker.
     *
     * <p>Inbound JSON ({@link JoinCarpoolDto}):</p>
     * <pre>{@code
     * {
     *   "departureTime": "2026-07-13T08:00:00",
     *   "rsOriginDestination": {
     *     "originLongitude": 36.807,
     *     "originLatitude": -1.283,
     *     "destinationLongitude": 36.812,
     *     "destinationLatitude": -1.300
     *   }
     * }
     * }</pre>
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Carpool joined successfully",
     *   "data": {
     *     "tripId": "uuid",
     *     "users": [
     *       {
     *         "userId": "uuid",
     *         "fullName": "Jane Doe",
     *         "email": "student@usiu.ac.ke",
     *         "role": "NORMAL_USER",
     *         "messagingToken": "optional-fcm-token"
     *       }
     *     ],
     *     "vehicle": {
     *       "vehicleId": "uuid",
     *       "user": {
     *         "userId": "uuid",
     *         "fullName": "Host Name",
     *         "email": "host@usiu.ac.ke",
     *         "role": "NORMAL_USER",
     *         "messagingToken": "optional-fcm-token"
     *       },
     *       "vehicleRegNumber": "KAA 123A",
     *       "vehicleDescription": "Silver Toyota Noah"
     *     },
     *     "tripCapacity": 3,
     *     "tripStatus": "OPEN",
     *     "originDestination": {
     *       "originLongitude": 36.807,
     *       "originLatitude": -1.283,
     *       "destinationLongitude": 36.812,
     *       "destinationLatitude": -1.300
     *     },
     *     "routePolyline": "encoded-polyline",
     *     "departureTime": "2026-07-13T08:00:00",
     *     "createdBy": "uuid",
     *     "destinationZone": "Westlands"
     *   }
     * }
     * }</pre>
     */
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

    /**
     *  Returns pending trip information for the authenticated user.
     *  <p>Inbound:</p>
     *  <ul>
     *      <li>{@code Authorization} header with a bearer token</li>
     *  </ul>
     *  <p>Outbound JSON:</p>
     *  <pre>{@code
     *  {
     *      "success": true,
     *      "message": "Operation successful",
     *      "data": {
     *          "tripId": "uuid",
     *          "tripStatus": "OPEN"
     *          }
     *      }
     *  }
     *  </pre>
     *
     *  @param authHeader the bearer token from the {@code Authorization} header
     *  @return a successful response containing the user's pending trip information
     *  */

    @GetMapping("/queryPendingTrips")
    public ResponseEntity<ApiResponse<TripInfo>> getTripInfo(@RequestHeader("Authorization") String authHeader){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Operation Successful" ,tripManagementService.getTripInfo(accessRecord.getUserId())));
    }

    /**
     * Returns pending carpool ride requests for the authenticated user.
     *
     * <p>Requires a valid JWT bearer token in the {@code Authorization} header.</p>
     *
     * <p>Response format:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Operation successful",
     *   "data": {
     *     "destinationZone": "Ngong",
     *     "requestMadeAt": localdateTime
     *   }
     * }
     * }</pre>
     *
     * @param authHeader bearer token from the {@code Authorization} header
     * @return {@link ResponseEntity} containing an {@code ApiResponse} with the user's ride request data
     * */
    @GetMapping("/queryCarpoolRequests")
    public ResponseEntity<ApiResponse<RideRequest>> getRideRequests(
            @RequestHeader("Authorization") String authHeader
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        RideRequest rideRequest = tripManagementService.getRideRequests(accessRecord.getUserId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Operation Successful", rideRequest));
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
