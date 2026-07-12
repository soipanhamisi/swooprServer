package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsiuCampusGeofenceServiceTests {

    private final UsiuCampusGeofenceService usiuCampusGeofenceService = new UsiuCampusGeofenceService();

    @Test
    @DisplayName("involvesUsiuCampus returns true when the origin is inside the USIU geofence")
    void involvesUsiuCampusReturnsTrueWhenOriginIsInsideCampus() {
        OriginDestination route = new OriginDestination(
                36.879300,
                -1.214500,
                36.820000,
                -1.280000
        );

        assertTrue(usiuCampusGeofenceService.involvesUsiuCampus(route));
    }

    @Test
    @DisplayName("involvesUsiuCampus returns true when the destination is inside the USIU geofence")
    void involvesUsiuCampusReturnsTrueWhenDestinationIsInsideCampus() {
        OriginDestination route = new OriginDestination(
                36.820000,
                -1.280000,
                36.879300,
                -1.214500
        );

        assertTrue(usiuCampusGeofenceService.involvesUsiuCampus(route));
    }

    @Test
    @DisplayName("involvesUsiuCampus returns false when neither point is inside the USIU geofence")
    void involvesUsiuCampusReturnsFalseWhenNeitherPointIsInsideCampus() {
        OriginDestination route = new OriginDestination(
                36.820000,
                -1.280000,
                36.900000,
                -1.200000
        );

        assertFalse(usiuCampusGeofenceService.involvesUsiuCampus(route));
    }

    @Test
    @DisplayName("involvesUsiuCampus returns false when route is null")
    void involvesUsiuCampusReturnsFalseWhenRouteIsNull() {
        assertFalse(usiuCampusGeofenceService.involvesUsiuCampus(null));
    }

    @Test
    @DisplayName("involvesUsiuCampus returns false when any route coordinate is null")
    void involvesUsiuCampusReturnsFalseWhenAnyCoordinateIsNull() {
        OriginDestination route = new OriginDestination(
                36.820000,
                null,
                36.900000,
                -1.200000
        );

        assertFalse(usiuCampusGeofenceService.involvesUsiuCampus(route));
    }
}


