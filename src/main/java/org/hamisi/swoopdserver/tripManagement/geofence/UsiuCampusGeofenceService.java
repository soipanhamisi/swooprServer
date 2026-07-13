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
            {36.8786388, -1.2205603},
            {36.8764807, -1.2185225},
            {36.8770082, -1.2143032},
            {36.8790465, -1.209892},
            {36.8796971, -1.207874},
            {36.8825528, -1.2092168},
            {36.8823636, -1.2112973},
            {36.8833512, -1.2142186},
            {36.8828215, -1.2141808},
            {36.8828215, -1.2155804},
            {36.8815351, -1.2166774},
            {36.8811324, -1.216842},
            {36.8806136, -1.2191883},
            {36.8800207, -1.2208924},
            {36.8786388, -1.2205603}
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


