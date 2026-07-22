package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RideRequest {
    private String destinationZone;
    private LocalDateTime requestMadeAt;
}
