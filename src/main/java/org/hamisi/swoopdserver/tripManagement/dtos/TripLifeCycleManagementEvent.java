package org.hamisi.swoopdserver.tripManagement.dtos;

import java.util.UUID;

public record TripLifeCycleManagementEvent(
        String status,
        String code,
        String message,
        UUID tripId
) {
    public static TripLifeCycleManagementEvent error(String code, String message){
        return new TripLifeCycleManagementEvent("ERROR", code, message, null);
    }
    public static TripLifeCycleManagementEvent progress(String code, String message){
        return new TripLifeCycleManagementEvent("PROGRESS", code, message, null);
    }
    public static TripLifeCycleManagementEvent success(UUID tripId){
        return new TripLifeCycleManagementEvent("SUCCESS", "TRIP_CREATED", "Trip created successfully", tripId);
    }
    public static TripLifeCycleManagementEvent success(String code, String message){
        return  new TripLifeCycleManagementEvent("SUCCESS", code, message, null);
    }
}
