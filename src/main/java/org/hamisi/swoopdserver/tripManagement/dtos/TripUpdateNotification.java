package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TripUpdateNotification {
    private int tripCapacity;
    private LocalDateTime departureTime;
    private OriginDestination originDestination;
    private TripStatus tripStatus;
    private String destinationZone;
    private String routePolyline;
    private List<String> carpoolMemberNames;
}

