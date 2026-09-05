package org.hamisi.swoopdserver.tripManagement.services;

import lombok.extern.slf4j.Slf4j;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.TripLifeCycleManagementEvent;
import org.hamisi.swoopdserver.tripManagement.entities.*;
import org.hamisi.swoopdserver.tripManagement.exceptions.NoAvailableTripException;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.repositories.RideSeekerBacklogRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripMembershipRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.users.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CarpoolMatchingService {

    private final FirebaseMessagingService firebaseMessagingService;
    private final TripRepository tripRepository;
    private final UsiuCampusGeofenceService usiuCampusGeofenceService;
    private final CarpoolMatchingTxService carpoolMatchingTxService;
    private final BacklogManagementService backlogManagementService;
    private final RideSeekerBacklogRepository rideSeekerBacklogRepository;
    private final PolylineProximityEvaluator polylineProximityEvaluator;
    private final UsersRepository usersRepository;
    private final TripMembershipRepository tripMembershipRepository;

    public CarpoolMatchingService(FirebaseMessagingService firebaseMessagingService, TripRepository tripRepository, UsiuCampusGeofenceService usiuCampusGeofenceService, CarpoolMatchingTxService carpoolMatchingTxService, BacklogManagementService backlogManagementService, RideSeekerBacklogRepository rideSeekerBacklogRepository, PolylineProximityEvaluator polylineProximityEvaluator, UsersRepository usersRepository, TripMembershipRepository tripMembershipRepository) {
        this.firebaseMessagingService = firebaseMessagingService;
        this.tripRepository = tripRepository;
        this.usiuCampusGeofenceService = usiuCampusGeofenceService;
        this.carpoolMatchingTxService = carpoolMatchingTxService;
        this.backlogManagementService = backlogManagementService;
        this.rideSeekerBacklogRepository = rideSeekerBacklogRepository;
        this.polylineProximityEvaluator = polylineProximityEvaluator;
        this.usersRepository = usersRepository;
        this.tripMembershipRepository = tripMembershipRepository;
    }

    @Transactional
    @Async("jobExecutor")
    public void matchRiderOrBacklog(UUID userId,
                                    LocalDateTime departureTime,
                                    OriginDestination originDestinationCoordinatePair){
        if (hasActiveTrip(userId) || backlogManagementService.hasBacklogRequest(userId)){
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "CARPOOL_MATCHING",
                    TripLifeCycleManagementEvent.error("PENDING_TRIP", "An active trip or request already exists")
            );
            return;
        }
        if (!usiuCampusGeofenceService.involvesUsiuCampus(originDestinationCoordinatePair)){
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "TRIP_CREATION",
                    TripLifeCycleManagementEvent.error("GEOFENCE_CHECK_FAILED",
                            "Request must include USIU Campus as either the origin or destination")
            );
            return;
        }
        Trip matchedTrip;
        try {
            matchedTrip = carpoolMatchingTxService.matchAndSaveTrip(userId, departureTime,originDestinationCoordinatePair);
        } catch (NoAvailableTripException e) {
            backlogManagementService.createBacklogRequest(
                    userId,
                    departureTime,
                    originDestinationCoordinatePair
            );
            return;
        }

        notifyCarpoolOnEntry(userId, matchedTrip.getUsers());

    }
    public Trip onBoardBackloggedUsers(Trip trip){
        List<RideSeekerBacklogEntry> rideRequests = rideSeekerBacklogRepository.findUnmatchedEntriesWithUsersOrderByRequestMadeAtAsc();
        if (rideRequests.isEmpty()){
            return trip;
        }

        //filter by a 30 minutes time window::
        LocalDateTime latest = trip.getDepartureTime().plusMinutes(15);
        LocalDateTime earliest = trip.getDepartureTime().minusMinutes(15);
        rideRequests.removeIf(rideSeekerBacklogEntry -> (rideSeekerBacklogEntry.getSelectedDepartureTime().isBefore(earliest)) ||
                (rideSeekerBacklogEntry.getSelectedDepartureTime().isAfter(latest)));
        if (rideRequests.isEmpty()){
            return trip;
        }

        //this should return a ranked list of rideRequest based on proximity routePolyline
        rideRequests = polylineProximityEvaluator.findBestMatch(trip.getRoutePolyline(), rideRequests);
        if (rideRequests.isEmpty()){
            return trip;
        }

        //add users to trip
        List<RideSeekerBacklogEntry> matchedRequests = new ArrayList<>();
        for (RideSeekerBacklogEntry rideSeekerBacklogEntry: rideRequests){
            if(trip.getTripStatus().equals(TripStatus.FULL)){
                break;
            }
            trip.addUser(rideSeekerBacklogEntry.getUser());
            trip.getTripMembership().add(
                    tripMembershipRepository.save(
                            new TripMembership().setPreferredDepartureTime(rideSeekerBacklogEntry.getSelectedDepartureTime())
                                    .setUser(rideSeekerBacklogEntry.getUser())
                                    .setTrip(trip)
                                    .setCoordinatePair(rideSeekerBacklogEntry.getOriginDestinationCoordinatePair())
                    )
            );
            notifyUserOnOnboarding(rideSeekerBacklogEntry.getUser().getUserId(), trip.getTripId());
            rideSeekerBacklogEntry.setMatched(true).setMatchedAt(LocalDateTime.now());
            matchedRequests.add(rideSeekerBacklogEntry);
        }
        rideSeekerBacklogRepository.saveAll(matchedRequests);
        tripRepository.save(trip);
        return trip;
    }

    private void notifyUserOnOnboarding(UUID userId, UUID tripId) {
        firebaseMessagingService.sendNotification(
                userId,
                "CARPOOL_MATCHING_SERVICE",
                "CARPOOL_FOUND",
                tripId
        );
    }

    private void notifyCarpoolOnEntry(UUID userId, List<User> users) {
        User carpoolEntrant = null;
        for (User user: users){
            if (user.getUserId().equals(userId)){
                carpoolEntrant = user;
                break;
            }
        }
        if (carpoolEntrant == null){
            log.debug("notifyCarpool called without entrant");
            return;
        }
        for (User user: users){
            if (user.getUserId().equals(userId)){
                carpoolEntrant = user;
                continue;
            }
            firebaseMessagingService.sendNotification(
                    user.getUserId(),
                    "CARPOOL_MATCHING_SERVICE",
                    "NEW_CARPOOL_MEMBER",
                    carpoolEntrant.getFullName() +" :has joined this carpool"

            );
        }
    }

    private boolean hasActiveTrip(UUID userId) {
        return tripRepository.belongsToAnOpenCarPool(userId);
    }
}
