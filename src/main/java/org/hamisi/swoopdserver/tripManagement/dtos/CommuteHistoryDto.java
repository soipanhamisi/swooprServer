package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommuteHistoryDto {
    private OriginDestination originDestinationCoordinates;
    private LocalDate departureDate;
    private LocalTime departureTime;
}

