package org.hamisi.swoopdserver.tripManagement.services;

import lombok.extern.slf4j.Slf4j;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.TripLifeCycleManagementEvent;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.exceptions.NoAvailableTripException;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.users.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CarpoolMatchingService {

    private final FirebaseMessagingService firebaseMessagingService;
    private final TripRepository tripRepository;
    private final UsiuCampusGeofenceService usiuCampusGeofenceService;
    private final CarpoolMatchingTxService carpoolMatchingTxService;

    public CarpoolMatchingService(FirebaseMessagingService firebaseMessagingService, TripRepository tripRepository, UsiuCampusGeofenceService usiuCampusGeofenceService, CarpoolMatchingTxService carpoolMatchingTxService) {
        this.firebaseMessagingService = firebaseMessagingService;
        this.tripRepository = tripRepository;
        this.usiuCampusGeofenceService = usiuCampusGeofenceService;
        this.carpoolMatchingTxService = carpoolMatchingTxService;
    }

    @Transactional
    @Async("jobExecutor")
    public void matchRiderOrBacklog(UUID userId,
                                    LocalDateTime departureTime,
                                    OriginDestination originDestinationCoordinatePair){
        if (hasActiveTrip(userId)){
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
            matchedTrip = carpoolMatchingTxService.matchAndSaveTrip(userId, originDestinationCoordinatePair);
        } catch (NoAvailableTripException e) {
//            TODO: Backlog ride request
            return;
        }

//      notify carpool a new rider has joined
        notifyCarpoolOnEntry(userId, matchedTrip.getUsers());

    }
    public int onBoardBackloggedUsers(){
//        TODO:
        return 1;
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
