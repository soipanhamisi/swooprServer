package org.hamisi.swoopdserver.tripManagement.exceptions;

public class NoRideRequestFoundException extends RuntimeException {
    public NoRideRequestFoundException(String message) {
        super(message);
    }
}
