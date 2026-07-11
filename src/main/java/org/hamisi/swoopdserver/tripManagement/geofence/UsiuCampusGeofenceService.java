package org.hamisi.swoopdserver.tripManagement.geofence;

import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.geom.Path2D;

@Service
public class UsiuCampusGeofenceService {
    private static final Logger log = LoggerFactory.getLogger(UsiuCampusGeofenceService.class);

    private static final double[][] USIU_PERIMETER_COORDINATES = {
            {36.8781661, -1.2132847},
            {36.8778294, -1.2140681},
            {36.8774805, -1.2152126},
            {36.877009,  -1.2166549},
            {36.8767839, -1.2172176},
            {36.877192,  -1.2186737},
            {36.8780644, -1.2198062},
            {36.8790613, -1.2204819},
            {36.8800956, -1.2200177},
            {36.8798986, -1.2185194},
            {36.8803207, -1.2173869},
            {36.8803418, -1.2158464},
            {36.8803137, -1.2146857},
            {36.8803974, -1.2135534},
            {36.880823,  -1.212374},
            {36.8820563, -1.2092917},
            {36.8801345, -1.2085453},
            {36.8788211, -1.2113513},
            {36.8783441, -1.2126713},
            {36.8781713, -1.2132726}
    };

    private static final Path2D FENCE_PATH = buildFencePath();

    public boolean involvesUsiuCampus(OriginDestination originDestination) {
        if (originDestination == null) {
            log.warn("USIU geofence check skipped because route coordinates are missing");
            return false;
        }

        if (originDestination.originLatitude() == null
                || originDestination.originLongitude() == null
                || originDestination.destinationLatitude() == null
                || originDestination.destinationLongitude() == null) {
            log.warn("USIU geofence check skipped because one or more coordinates are null");
            return false;
        }

        boolean originInside = isInsideCampus(originDestination.originLatitude(), originDestination.originLongitude());
        boolean destinationInside = isInsideCampus(originDestination.destinationLatitude(), originDestination.destinationLongitude());
        boolean involvesCampus = originInside || destinationInside;

        log.debug(
                "USIU geofence check: origin(lat={}, lon={}) inside={}, destination(lat={}, lon={}) inside={}, involvesCampus={}",
                originDestination.originLatitude(),
                originDestination.originLongitude(),
                originInside,
                originDestination.destinationLatitude(),
                originDestination.destinationLongitude(),
                destinationInside,
                involvesCampus
        );

        return involvesCampus;
    }

    boolean isInsideCampus(double latitude, double longitude) {
        return FENCE_PATH.contains(longitude, latitude);
    }

    private static Path2D buildFencePath() {
        Path2D fencePath = new Path2D.Double();
        fencePath.moveTo(USIU_PERIMETER_COORDINATES[0][0], USIU_PERIMETER_COORDINATES[0][1]);
        for (int i = 1; i < USIU_PERIMETER_COORDINATES.length; i++) {
            fencePath.lineTo(USIU_PERIMETER_COORDINATES[i][0], USIU_PERIMETER_COORDINATES[i][1]);
        }
        fencePath.closePath();
        return fencePath;
    }
}


