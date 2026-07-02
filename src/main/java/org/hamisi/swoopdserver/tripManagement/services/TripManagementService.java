package org.hamisi.swoopdserver.tripManagement.services;

import lombok.SneakyThrows;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.entities.Vehicle;
import org.hamisi.swoopdserver.tripManagement.proxies.GoogleRoutesProxy;
import org.hamisi.swoopdserver.tripManagement.records.UserDestinationZone;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.VehicleRepository;
import org.hamisi.swoopdserver.users.User;
import org.springframework.stereotype.Service;

import java.awt.geom.Path2D;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TripManagementService {
    private static final Path2D FENCE_PATH;
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
    private final UsersRepository usersRepository;
    private final VehicleRepository vehicleRepository;
    private final GoogleRoutesProxy googleRoutesProxy;
    private final TripRepository tripRepository;
    private final ConcurrentLinkedQueue<UserDestinationZone> rideSeekerBacklog = new ConcurrentLinkedQueue<>();

    static {
        FENCE_PATH = buildFencePath();
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

    private final FirebaseMessagingService firebaseMessagingService;

    public TripManagementService(UsersRepository usersRepository,
                                 VehicleRepository vehicleRepository,
                                 GoogleRoutesProxy googleRoutesProxy,
                                 TripRepository tripRepository,
                                 FirebaseMessagingService firebaseMessagingService) {
        this.usersRepository = usersRepository;
        this.vehicleRepository = vehicleRepository;
        this.googleRoutesProxy = googleRoutesProxy;
        this.tripRepository = tripRepository;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    public void registerVehicle(UUID userId, VehicleDto vehicleDto){
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleRegNumber(vehicleDto.getRegNo());
        vehicle.setVehicleDescription(vehicleDto.getDesc());
        vehicle.setUser(usersRepository.getReferenceById(userId));
        vehicleRepository.save(vehicle);
    }


    public void createTrip(
            UUID userId,
            int tripCapacity,
            LocalDateTime departureTime,
            OriginDestination originDestination
    ){

        if (!checkForUsiuDestinationOriginHelper(originDestination)){
            throw new CannotCreateTripException("Cannot create trips not involving the USIU campus");
        }
        if (!vehicleRepository.getVehiclesByUser_UserId(userId)){
            throw new CannotCreateTripException("No registered vehicle");
        }
        Trip trip = new Trip();
        trip.setVehicle(vehicleRepository.findVehicleByUser_UserId(userId));
        trip.setTripCapacity(tripCapacity);
        trip.setUsers(new ArrayList<>());
        trip.setDepartureTime(departureTime);
        trip.setOriginDestination(originDestination);
        trip.setCreatedBy(userId);
        trip.setTripStatus(TripStatus.OPEN);
        trip.setDestinationZone(
                googleRoutesProxy.getDestinationZone(
                        originDestination.destinationLatitude(),
                originDestination.destinationLongitude()
                )
        );
        trip.setRoutePolyline(googleRoutesProxy.getRoute(originDestination));
        onboardBackloggedRideSeekersHelper(trip);
        tripRepository.save(trip);

    }

    public void cancelTrip(UUID userId)  {
        Trip trip = tripRepository.getTripByCreatedBy(userId);

        if(trip == null || trip.getTripStatus() != TripStatus.OPEN){
            throw new CannotCancelTripException("cannot cancel trip");
        }

        if (trip.getUsers() != null && !trip.getUsers().isEmpty()) {
            String cancellationMsg = "Your trip has been cancelled by carpool host." +
                    " You have been placed in a backlog and will be notified if another trip is available";
            for (User user : trip.getUsers()) {
                firebaseMessagingService.sendNotification(user.getUserId(), cancellationMsg);
                addRStoBacklogHelper(new UserDestinationZone(user, trip.getDestinationZone(), trip.getDepartureTime()));
            }
        }

        trip.setTripStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

        public Trip joinCarpool(UUID userId, LocalDateTime departureTime, OriginDestination rsDestination) {
        String destinationZone = googleRoutesProxy.getDestinationZone(rsDestination.destinationLatitude(),
                rsDestination.destinationLongitude());
        List<Trip> potentialTrips = tripRepository.getTripsByTripStatusDestinationZonedTime(TripStatus.OPEN, destinationZone, departureTime);

        if (potentialTrips.isEmpty()) {
            addRStoBacklogHelper(new UserDestinationZone(usersRepository.getUserByUserId(userId), destinationZone, departureTime));
            throw new NoAvailableTripException("There are no open trips currently. " +
                    "You will be notified if a new trip is available");
        }

        Trip trip = potentialTrips.getFirst();
        if (trip.getUsers() == null) {
            trip.setUsers(new ArrayList<>());
        }
        trip.addUser(usersRepository.getUserByUserId(userId));
        tripRepository.save(trip);

        List<User> carpool = tripRepository.getTripUsersByTripId(trip.getTripId());
        String joinNotification = " has joined the carpool";
        for(User user: carpool){
            if (user.getUserId().equals(userId)){
                continue;
            }
            firebaseMessagingService.sendNotification(
                    user.getUserId(),
                    usersRepository.getFullNameByUserId(userId) + joinNotification);
        }
        return trip;

    }
    /**
     * Checks if either the origin or destination lies inside the USIU perimeter.
     * Path2D treats X as Longitude and Y as Latitude.
     */
    private boolean checkForUsiuDestinationOriginHelper(OriginDestination originDestination) {
        boolean isOriginInside = FENCE_PATH.contains(
                originDestination.originLongitude(),
                originDestination.originLatitude()
        );

        boolean isDestinationInside = FENCE_PATH.contains(
                originDestination.destinationLongitude(),
                originDestination.destinationLatitude()
        );

        return isOriginInside || isDestinationInside;
    }

    public void addRStoBacklogHelper(UserDestinationZone user){
        if (user == null || user.getUser() == null || user.getDestinationZone() == null || user.getPreferredDepartureTime() == null) {
            throw new IllegalArgumentException("Backlog entry is incomplete");
        }
        rideSeekerBacklog.add(user);
    }

    public User getRideSeekerFromBacklogHelper(LocalDateTime dateTime, String destinationZone){
        for (UserDestinationZone userDestinationZone : rideSeekerBacklog) {
            boolean sameZone = Objects.equals(destinationZone, userDestinationZone.getDestinationZone());
            boolean sameDeparture = Objects.equals(dateTime, userDestinationZone.getPreferredDepartureTime());
            if (sameZone && sameDeparture && rideSeekerBacklog.remove(userDestinationZone)) {
                return userDestinationZone.getUser();
            }
        }
        return null;
    }

    @SneakyThrows
    private void onboardBackloggedRideSeekersHelper(Trip trip)  {
        while (trip.getTripCapacity() > 0) {
            User backloggedUser = getRideSeekerFromBacklogHelper(trip.getDepartureTime(), trip.getDestinationZone());
            if (backloggedUser == null) {
                break;
            }
            trip.addUser(backloggedUser);
            firebaseMessagingService.sendNotification(backloggedUser.getUserId(), "You have been matched to a new carpool");
        }
    }
}
