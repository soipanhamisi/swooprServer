package org.hamisi.swoopdserver.tripManagement.services;

public class RegisterVehicleException extends RuntimeException {
    public RegisterVehicleException(String wrongNumberPlateFormat) {
        super(wrongNumberPlateFormat);
    }
}
