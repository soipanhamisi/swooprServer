package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TripCreationDTO {
    private String email;
    private int tripCapacity;
    private LocalDateTime departureTime;
    private Long destinationLongitude;
    private Long destinationLatitude;
    private Long originLongitude;
    private Long originLatitude;

}
