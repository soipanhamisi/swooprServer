package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.tripManagement.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception responses for trip-management endpoints.
 *
 * <p>All handlers return the shared {@code ApiResponse} failure envelope:</p>
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Reason for failure",
 *   "data": null
 * }
 * }</pre>
 */
@RestControllerAdvice
public class TripManagementControllerExceptionHandlers {

    /**
     * Returned when a trip cannot be created for the current request.
     */
    @ExceptionHandler(CannotCreateTripException.class)
    public ResponseEntity<ApiResponse<Void>> handleTripCreationExceptions(CannotCreateTripException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when a trip cannot be cancelled.
     */
    @ExceptionHandler(CannotCancelTripException.class)
    public ResponseEntity<ApiResponse<Void>> handleTripCancellationExceptions(CannotCancelTripException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when the system cannot find a matching trip.
     */
    @ExceptionHandler(NoAvailableTripException.class)
    public ResponseEntity<ApiResponse<Void>>handleNoAvailableTripsException(NoAvailableTripException ex){
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when a carpool join request cannot be created.
     */
    @ExceptionHandler(CannotCreateCarpoolRequestException.class)
    public ResponseEntity<ApiResponse<Void>>cannotCreateCarpoolRequestException(
            CannotCreateCarpoolRequestException ex
    ){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when Google Maps services are temporarily unavailable.
     */
    @ExceptionHandler(GoogleMapsServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleGoogleMapsServiceUnavailableException(
            GoogleMapsServiceUnavailableException ex
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(TripInfoException.class)
    public ResponseEntity<ApiResponse<Void>> handleTripInfo(TripInfoException ex){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(ex.getMessage()));
    }


}
