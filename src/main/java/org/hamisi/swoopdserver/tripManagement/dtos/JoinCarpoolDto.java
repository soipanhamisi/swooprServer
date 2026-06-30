package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;

import java.time.LocalDateTime;

@Getter
@Setter
public class JoinCarpoolDto {
    private LocalDateTime departureTime;
    private OriginDestination rsOriginDestination;
}
