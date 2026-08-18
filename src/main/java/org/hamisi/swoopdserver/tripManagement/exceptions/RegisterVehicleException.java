package org.hamisi.swoopdserver.tripManagement.exceptions;

public class RegisterVehicleException extends RuntimeException {
    public RegisterVehicleException(String wrongNumberPlateFormat) {
        super(wrongNumberPlateFormat);
    }
}
