package org.hamisi.swoopdserver.tripManagement.exceptions;

public class TripInfoException extends RuntimeException {
    public TripInfoException(String notCurrentlyInAnyTrip) {
        super(notCurrentlyInAnyTrip);
    }
}
