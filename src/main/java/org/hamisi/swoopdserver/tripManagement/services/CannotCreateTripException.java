package org.hamisi.swoopdserver.tripManagement.services;

public class CannotCreateTripException extends RuntimeException {
    public CannotCreateTripException(String s) {
        super(s);
    }
}
