package org.hamisi.swoopdserver.tripManagement.services;

public class NoRideRequestFoundException extends RuntimeException {
    public NoRideRequestFoundException(String message) {
        super(message);
    }
}
