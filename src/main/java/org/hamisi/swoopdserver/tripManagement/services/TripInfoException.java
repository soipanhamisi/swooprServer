package org.hamisi.swoopdserver.tripManagement.services;

public class TripInfoException extends RuntimeException {
    public TripInfoException(String notCurrentlyInAnyTrip) {
        super(notCurrentlyInAnyTrip);
    }
}
