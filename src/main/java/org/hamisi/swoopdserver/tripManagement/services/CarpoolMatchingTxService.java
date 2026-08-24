package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.tripManagement.entities.*;
import org.hamisi.swoopdserver.tripManagement.exceptions.NoAvailableTripException;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.repositories.TripMembershipRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CarpoolMatchingTxService {
    private final TripRepository tripRepository;
    private final UsiuCampusGeofenceService usiuCampusGeofenceService;
    private final PolylineProximityEvaluator polylineProximityEvaluator;
    private final UsersRepository usersRepository;
    private final TripMembershipRepository tripMembershipRepository;

    public CarpoolMatchingTxService(TripRepository tripRepository, UsiuCampusGeofenceService usiuCampusGeofenceService, PolylineProximityEvaluator polylineProximityEvaluator, UsersRepository usersRepository, TripMembershipRepository tripMembershipRepository) {
        this.tripRepository = tripRepository;
        this.usiuCampusGeofenceService = usiuCampusGeofenceService;
        this.polylineProximityEvaluator = polylineProximityEvaluator;
        this.usersRepository = usersRepository;
        this.tripMembershipRepository = tripMembershipRepository;
    }

    @Transactional
    public Trip matchAndSaveTrip(UUID userId, LocalDateTime departureTime, OriginDestination originDestinationCoordinatePair) {
        TripDirection tripDirection = usiuCampusGeofenceService.resolveTripDirection(originDestinationCoordinatePair);
        List<Trip> trips = tripRepository.getAllOpenTripsByTripDirection(tripDirection);
        Optional<Trip> trip = polylineProximityEvaluator.findBestMatch(originDestinationCoordinatePair, trips);

        if (trip.isEmpty()) {
            throw new NoAvailableTripException("No optimal trips found");
        }

        Trip matchedTrip = trip.get();
        boolean alreadyMember = matchedTrip.getUsers().stream()
                        .anyMatch(u -> u.getUserId().equals(userId));
        if (alreadyMember){
            return matchedTrip;
        }
        matchedTrip.getUsers().addFirst(usersRepository.getReferenceById(userId));
        TripMembership tripMembership = tripMembershipRepository.save(
                new TripMembership().setTrip(matchedTrip)
                        .setUser(usersRepository.getReferenceById(userId))
                        .setCoordinatePair(originDestinationCoordinatePair)
                        .setPreferredDepartureTime(departureTime)
        );
        matchedTrip.getTripMembership().add(tripMembership);
        if (matchedTrip.getTripCapacity() >= matchedTrip.getUsers().size()) {
            matchedTrip.setTripStatus(TripStatus.FULL);
        }
        return tripRepository.save(matchedTrip);
    }
}
