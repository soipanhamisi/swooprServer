package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.tripManagement.services.CannotCancelTripException;
import org.hamisi.swoopdserver.tripManagement.services.CannotCreateTripException;
import org.hamisi.swoopdserver.tripManagement.services.NoAvailableTripException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TripManagementControllerExceptionHandlers {

    @ExceptionHandler(CannotCreateTripException.class)
    public ResponseEntity<String>handleTripCreationExceptions(CannotCreateTripException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
    @ExceptionHandler(CannotCancelTripException.class)
    public ResponseEntity<String>handleTripCancellationExceptions(CannotCancelTripException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
    @ExceptionHandler(NoAvailableTripException.class)
    public ResponseEntity<String>handleNoAvailableTripsException(NoAvailableTripException ex){
        return ResponseEntity.status(HttpStatus.CONTINUE).body(ex.getMessage());
    }


}
