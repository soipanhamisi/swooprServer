package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripDirection;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.exceptions.NoAvailableTripException;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CarpoolMatchingTxService {
    private final TripRepository tripRepository;
    private final UsiuCampusGeofenceService usiuCampusGeofenceService;
    private final PolylineProximityEvaluator polylineProximityEvaluator;
    private final UsersRepository usersRepository;

    public CarpoolMatchingTxService(TripRepository tripRepository, UsiuCampusGeofenceService usiuCampusGeofenceService, PolylineProximityEvaluator polylineProximityEvaluator, UsersRepository usersRepository) {
        this.tripRepository = tripRepository;
        this.usiuCampusGeofenceService = usiuCampusGeofenceService;
        this.polylineProximityEvaluator = polylineProximityEvaluator;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public Trip matchAndSaveTrip(UUID userId, OriginDestination originDestinationCoordinatePair) {
        TripDirection tripDirection = usiuCampusGeofenceService.resolveTripDirection(originDestinationCoordinatePair);
        List<Trip> trips = tripRepository.getAllOpenTripsByTripDirection(tripDirection);
        Optional<Trip> trip = polylineProximityEvaluator.findBestMatch(originDestinationCoordinatePair, trips);

        if (trip.isEmpty()) {
            throw new NoAvailableTripException("No optimal trips found");
        }

        Trip matchedTrip = trip.get();
        matchedTrip.getUsers().addFirst(usersRepository.getReferenceById(userId));
        if (matchedTrip.getTripCapacity() == matchedTrip.getUsers().size()) {
            matchedTrip.setTripStatus(TripStatus.FULL);
        }
        return tripRepository.save(matchedTrip);
    }
}
