package org.hamisi.swoopdserver.tripManagement.services;

import lombok.extern.slf4j.Slf4j;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.TripData;
import org.hamisi.swoopdserver.tripManagement.dtos.TripLifeCycleManagementEvent;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.*;
import org.hamisi.swoopdserver.tripManagement.exceptions.CannotCreateTripException;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.proxies.GoogleRoutesProxy;
import org.hamisi.swoopdserver.tripManagement.repositories.TripMembershipRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.VehicleRepository;
import org.hamisi.swoopdserver.users.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TripLifecycleManagementService {

    private final FirebaseMessagingService firebaseMessagingService;
    private final VehicleRepository vehicleRepository;
    private final UsiuCampusGeofenceService usiuCampusGeofenceService;
    private final TripRepository tripRepository;
    private final GoogleRoutesProxy googleRoutesProxy;
    private final UsersRepository usersRepository;
    private final CarpoolMatchingService carpoolMatchingService;
    private final BacklogManagementService backlogManagementService;
    private final TripMembershipRepository tripMembershipRepository;

    public TripLifecycleManagementService(FirebaseMessagingService firebaseMessagingService, VehicleRepository vehicleRepository, UsiuCampusGeofenceService usiuCampusGeofenceService, TripRepository tripRepository, GoogleRoutesProxy googleRoutesProxy, UsersRepository usersRepository, CarpoolMatchingService carpoolMatchingService, BacklogManagementService backlogManagementService, TripMembershipRepository tripMembershipRepository) {
        this.firebaseMessagingService = firebaseMessagingService;
        this.vehicleRepository = vehicleRepository;
        this.usiuCampusGeofenceService = usiuCampusGeofenceService;
        this.tripRepository = tripRepository;
        this.googleRoutesProxy = googleRoutesProxy;
        this.usersRepository = usersRepository;
        this.carpoolMatchingService = carpoolMatchingService;
        this.backlogManagementService = backlogManagementService;
        this.tripMembershipRepository = tripMembershipRepository;
    }

    @Async("jobExecutor")
    public void createTrip(UUID userId,
                           int tripCapacity,
                           LocalDateTime departureTime,
                           VehicleDto vehicleData,
                           OriginDestination originDestinationCoordinatePair)
    {
        //      Check if the user has an open/full/in_progress trip
        if (hasActiveTrip(userId)){
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "TRIP_CREATION",
                    TripLifeCycleManagementEvent.error("PENDING_TRIP", "An active trip or request already exists")
            );
            return;
        }

        //      Check vehicle validity,send fcm message on success/failure
        if (!validateVehicleOwnership(userId, vehicleData)) {
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "TRIP_CREATION",
                    TripLifeCycleManagementEvent.error("VEHICLE_VALIDATION_FAILED", "Could not validate vehicle ownership")
            );
            return;
        }
        Vehicle vehicle = vehicleRepository.findByUserAndVehicleRegNumber(
                usersRepository.getReferenceById(userId),
                vehicleData.getRegNo()
        );

        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CREATION",
                TripLifeCycleManagementEvent.progress("VEHICLE_VALIDATED", "Vehicle ownership confirmed")
                );

        //      Check the OriginDestinationCoordinatePair against geofencing rules send fcm message on success/failure
        if (!usiuCampusGeofenceService.involvesUsiuCampus(originDestinationCoordinatePair)){
            firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CREATION",
                TripLifeCycleManagementEvent.error("GEOFENCE_CHECK_FAILED", "Trip must include USIU Campus as either the origin or destination")
            );
            return;
        }
        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CREATION",
                TripLifeCycleManagementEvent.progress("GEOFENCE_CHECK_SUCESSFULL", "Trip involves USIU campus")
        );

        //      Resolve the originDestinationCoordinatePair to neighbourhoodZones using googleMaps API, send fcm message on success/failure
        String originZone = googleRoutesProxy.getNeighborhoodZone(
                originDestinationCoordinatePair.originLatitude(),
                originDestinationCoordinatePair.originLongitude()
        );
        String destinationZone = googleRoutesProxy.getNeighborhoodZone(
                originDestinationCoordinatePair.destinationLatitude(),
                originDestinationCoordinatePair.destinationLongitude()
        );
        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CREATION",
                TripLifeCycleManagementEvent.progress("ORIGIN_DESTINATION_NEIGHBORHOOD_RESOLUTION_COMPLETE",
                        originZone + ", " +destinationZone)
        );

        //      Use the Google Routes API to obtain the route polyline from origin to destination, send fcm message on success/failure
        String routePolyline = googleRoutesProxy.getRoute(
                originDestinationCoordinatePair
        );
        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CREATION",
                TripLifeCycleManagementEvent.progress("ROUTE_POLYLINE_RESOLUTION_COMPLETE", routePolyline)
        );

        //      save the trip to db and send success message
        Trip trip = new Trip();
        trip.setCreatedBy(userId);
        trip.setTripCapacity(tripCapacity);
        trip.setVehicle(vehicle);
        trip.setDepartureTime(departureTime);
        trip.setOriginDestination(originDestinationCoordinatePair);
        trip.setRoutePolyline(routePolyline);
        trip.setOriginZone(originZone);
        trip.setDestinationZone(destinationZone);
        trip.setTripDirection(usiuCampusGeofenceService.resolveTripDirection(
                originDestinationCoordinatePair
        ));
        trip.addUser(usersRepository.getReferenceById(userId));
        trip.getTripMembership().add(
                new TripMembership().setCoordinatePair(originDestinationCoordinatePair)
                        .setUser(usersRepository.getReferenceById(userId))
                        .setTrip(trip)
                        .setPreferredDepartureTime(departureTime)
        );
        Trip savedTrip;
        try {
            savedTrip = tripRepository.save(trip);
            savedTrip.getTripMembership().add(
                    tripMembershipRepository.save(
                            new TripMembership().setCoordinatePair(originDestinationCoordinatePair)
                                    .setPreferredDepartureTime(departureTime)
                                    .setUser(usersRepository.getReferenceById(userId))
                                    .setTrip(savedTrip)
                    )
            );
            carpoolMatchingService.onBoardBackloggedUsers(tripRepository.save(savedTrip));
        } catch (DataIntegrityViolationException e) {
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "TRIP_CREATION",
                    TripLifeCycleManagementEvent.error("DATA_INTEGRITY_ERROR", "Could not create trip")
            );
            log.error(e.getMessage());
            throw new CannotCreateTripException("Could not create trip");
        }

        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CREATION",
                TripLifeCycleManagementEvent.success(savedTrip.getTripId())
        );
    }
    @Async("jobExecutor")
    @Transactional
    public void cancelTrip(UUID userId, UUID tripId){
        Trip trip =tripRepository.getReferenceById(tripId);

        //check trip is open/full
        if (!(trip.getTripStatus().equals(TripStatus.OPEN) || trip.getTripStatus().equals(TripStatus.FULL))) {
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "TRIP_CANCELLATION",
                    TripLifeCycleManagementEvent.error(
                            "TRIP_CANCELLATION_FAILED",
                            "Trip cancellation is only allowed for OPEN or FULL trips"
                    )
            );
            return;
        }

        //check user is indeed carpool owner
        if (!trip.getCreatedBy().equals(userId)){
            firebaseMessagingService.sendNotification(
                    userId,
                    "TRIP_MANAGEMENT",
                    "TRIP_CANCELLATION",
                    TripLifeCycleManagementEvent.error(
                            "TRIP_CANCELLATION_FAILED",
                            "Only the trip host can cancel this trip."
                    )
            );
            return;
        }
        //notify all the trip users of cancellation
        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CANCELLATION",
                TripLifeCycleManagementEvent.progress(
                        "CANCELLATION_NOTIFICATION_STARTED",
                        "Sending cancellation notifications to riders"
                )
        );
        notifyUsers(trip);
        //mark trip as canceled
        trip.setTripStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
        firebaseMessagingService.sendNotification(
                userId,
                "TRIP_MANAGEMENT",
                "TRIP_CANCELLATION",
                TripLifeCycleManagementEvent.success(
                        "CANCELLATION_COMPLETE",
                        "Trip cancelled successfully"
                )
        );
//       Backlog the users in the carpool
        backlogCancelledUsers(trip);
    }

    public void leaveCarpool(UUID userId, UUID tripId) {
        Trip trip = tripRepository.getReferenceById(tripId);

        if (
                trip.getTripStatus().equals(TripStatus.COMPLETED) || trip.getTripStatus().equals(TripStatus.IN_PROGRESS)
        ){
            return;
        }

        User user = usersRepository.getReferenceById(userId);
        List<TripMembership> tripMembershipList = trip.getTripMembership();
        for (TripMembership tripMembershipRecord: tripMembershipList){
            if(tripMembershipRecord.getUser().equals(user)){
                tripMembershipList.remove(tripMembershipRecord);
                break;
            }
        }
        trip.getUsers().remove(user);

        if (!(trip.getTripCapacity() >= trip.getUsers().size())){
            trip.setTripStatus(TripStatus.OPEN);
        }
        notifyUsersOfCarpoolExit(tripRepository.save(trip), user.getFullName());
    }

    @Async("jobExecutor")
    public void notifyUsersOfCarpoolExit(Trip trip, String exitingUsername){
        for (User user: trip.getUsers()){
           firebaseMessagingService.sendNotification(
                    user.getUserId(),
                    "TRIP_MANAGEMENT",
                    "TIP_EXIT",
                    exitingUsername + " has left the carpool."
            );
        }
    }

    private void backlogCancelledUsers(Trip trip) {
        for(TripMembership tripMembershipRecord: trip.getTripMembership()){
            if(tripMembershipRecord.getUser().getUserId().equals(trip.getCreatedBy())){
             continue;
            }
            backlogManagementService.createBacklogRequest(
                    tripMembershipRecord.getUser().getUserId(),
                    tripMembershipRecord.getPreferredDepartureTime(),
                    tripMembershipRecord.getCoordinatePair()
            );
            firebaseMessagingService.sendNotification(
                    tripMembershipRecord.getUser().getUserId(),
                    "TRIP_MANAGEMENT",
                    "TRIP_CANCELLATION",
                    TripLifeCycleManagementEvent.progress("BACKLOG_REQUEUE", "The Carpool has been Cancelled by carpool owner. You have been placed in a backlog")
            );
        }
    }

    public TripData getTripInfo(UUID tripId){
        Trip trip = tripRepository.getReferenceById(tripId);
        return parseTripData(trip);
    }

    private TripData parseTripData(Trip trip) {
        TripData tripData = new TripData();
        tripData.setOriginDestinationCoordinates(
                trip.getOriginDestination()
        );
        tripData.setDepartureTime(trip.getDepartureTime());
        tripData.setCapacity(trip.getTripCapacity());
        VehicleDto  vehicleDto = new VehicleDto();
        vehicleDto.setDesc(trip.getVehicle().getVehicleDescription());
        vehicleDto.setRegNo(trip.getVehicle().getVehicleRegNumber());
        tripData.setVehicle(vehicleDto);
        return tripData;
    }

    private boolean hasActiveTrip(UUID userId) {
        return tripRepository.belongsToAnOpenCarPool(userId);
    }

    private boolean validateVehicleOwnership(UUID userId, VehicleDto vehicleData) {
        List<Vehicle> vehicles =vehicleRepository.findVehicleByUser_UserId(userId);
        for (Vehicle vehicle: vehicles){
            if (vehicle.getVehicleRegNumber().equalsIgnoreCase(vehicleData.getRegNo())){
                return true;
            }
        }
        return false;
    }

    private void notifyUsers(Trip trip) {
        UUID tripOwnerId = trip.getCreatedBy();
        for (User user: trip.getUsers()){
            if (user.getUserId().equals(tripOwnerId)){
                continue;
            }
            firebaseMessagingService.sendNotification(
                    user.getUserId(),
                    "TRIP_MANAGEMENT",
                    "TRIP_CANCELLATION",
                    TripLifeCycleManagementEvent.success(
                            "TRIP_CANCELLATION_ALERT",
                            "Host has cancelled this carpool"
                    )
            );
        }
    }
}
