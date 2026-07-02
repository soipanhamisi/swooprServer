package org.hamisi.swoopdserver.tripManagement.services;

public class NoAvailableTripException extends RuntimeException {
    public NoAvailableTripException(String s) {
        super(s);
    }
}
