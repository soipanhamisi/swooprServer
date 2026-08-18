package org.hamisi.swoopdserver.tripManagement.exceptions;

public class CannotCancelTripException extends RuntimeException {
    public CannotCancelTripException(String cannotCancelTrip) {
        super(cannotCancelTrip);
    }
}
