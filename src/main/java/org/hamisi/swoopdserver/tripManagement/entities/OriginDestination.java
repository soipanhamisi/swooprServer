package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
public record OriginDestination(
        Double originLongitude,
        Double originLatitude,
        Double destinationLongitude,
        Double destinationLatitude
) {}
