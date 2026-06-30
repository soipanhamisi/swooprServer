package org.hamisi.swoopdserver.tripManagement.services;

public class CannotCancelTripException extends RuntimeException {
    public CannotCancelTripException(String cannotCancelTrip) {
        super(cannotCancelTrip);
    }
}
