package org.hamisi.swoopdserver.tripManagement.services;

import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.LatLng;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PolylineProximityEvaluator {
    private static final double PROXIMITY_THRESHOLD = 10.0;

    public Optional<Trip> findBestMatch(OriginDestination coordinatePair,
                                        List<Trip> tripList) {
        if (tripList.isEmpty()) {
            return Optional.empty();
        }
        Map<Trip, Double> distanceCostTable = new HashMap<>();
        for (Trip trip : tripList) {
            List<LatLng> coordinateList = PolylineEncoding.decode(trip.getRoutePolyline());
            distanceCostTable.put(trip, findShortestEuclideanDistance(coordinatePair, coordinateList));
        }
        Double minDistance = Collections.min(distanceCostTable.values());
        if (minDistance > PROXIMITY_THRESHOLD) {
            return Optional.empty();
        } else {
            return distanceCostTable.entrySet().stream()
                    .filter(entry -> entry.getValue().doubleValue() == minDistance.doubleValue())
                    .map(Map.Entry::getKey)
                    .findFirst();
        }
    }

    private Double findShortestEuclideanDistance(OriginDestination targetCoordinatePair, List<LatLng> coordinateList) {
        if (coordinateList.isEmpty()) {
            return Double.MAX_VALUE;
        }
        ArrayList<Double> distances = new ArrayList<>();
        for (LatLng point: coordinateList){
            distances.add(
                    Math.sqrt(Math.pow(targetCoordinatePair.originLatitude() - point.lat, 2) + Math.pow(targetCoordinatePair.originLongitude() - point.lng, 2))
            );
        }
        return Collections.min(distances);
    }
}
