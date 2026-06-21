package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.Embeddable;

@Embeddable
public record OriginDestination(
        Double originLongitude,
        Double originLatitude,
        Double destinationLongitude,
        Double destinationLatitude
) {}
