package org.hamisi.swoopdserver.tripManagement.services;

import jakarta.transaction.Transactional;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
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
    private final UsersRepository usersRepository;
    private final VehicleRepository vehicleRepository;
    private final GoogleRoutesProxy googleRoutesProxy;
    private final TripRepository tripRepository;
    private final ConcurrentLinkedQueue<UserDestinationZone> rideSeekerBacklog = new ConcurrentLinkedQueue<>();

    static {
        FENCE_PATH = new Path2D.Double();

        // Structural nodes defining the USIU perimeter boundaries
        double[][] polygonCoordinates = {
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

        // Plot the initial starting position
        FENCE_PATH.moveTo(polygonCoordinates[0][0], polygonCoordinates[0][1]);

        // Thread the path lines through the remaining nodes
        for (int i = 1; i < polygonCoordinates.length; i++) {
            FENCE_PATH.lineTo(polygonCoordinates[i][0], polygonCoordinates[i][1]);
        }

        // Seal the polygon boundary
        FENCE_PATH.closePath();
    }

    public TripManagementService(UsersRepository usersRepository, VehicleRepository vehicleRepository, GoogleRoutesProxy googleRoutesProxy, TripRepository tripRepository) {
        this.usersRepository = usersRepository;
        this.vehicleRepository = vehicleRepository;
        this.googleRoutesProxy = googleRoutesProxy;
        this.tripRepository = tripRepository;
    }

    public void createTrip(
            String vehicleRegNo,
            String vehicleDesc,
            UUID userId,
            int tripCapacity,
            LocalDateTime departureTime,
            OriginDestination originDestination
    ){
        if (vehicleRegNo.isEmpty() || vehicleDesc.isEmpty()){
            throw new IllegalArgumentException("insufficient vehicle information");
        }
        if (!checkForUsiuDestinationOrigin(originDestination)){
            throw new IllegalArgumentException("Cannot create trips not involving the USIU campus");
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleRegNumber(vehicleRegNo);
        vehicle.setVehicleDescription(vehicleDesc);
        vehicle.setUser(usersRepository.getUserByUserId(userId));
        vehicleRepository.save(vehicle);
        Trip trip = new Trip();
        trip.setVehicle(vehicle);
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
        onboardBackloggedRideSeekers(trip);
        tripRepository.save(trip);

    }

    public void cancelTrip(UUID userId){
        Trip trip = tripRepository.getTripByCreatedBy(userId);
        if(trip == null || trip.getTripStatus() != TripStatus.OPEN){
            throw new IllegalArgumentException("cannot cancel trip");
        }
        if (trip.getUsers() != null && !trip.getUsers().isEmpty()) {
            for (User user : trip.getUsers()) {
                addRStoBacklog(new UserDestinationZone(user, trip.getDestinationZone(), trip.getDepartureTime()));
            }
        }
        trip.setTripStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

    @Transactional
    public void joinCarpool(
            UUID userId,
            LocalDateTime departureTime,
            OriginDestination rsDestination
    ) {
        String destinationZone = googleRoutesProxy.getDestinationZone(rsDestination.destinationLatitude(),
                rsDestination.destinationLongitude());
        List<Trip> potentialTrips = tripRepository.getTripsByTripStatusDestinationZonedTime(TripStatus.OPEN, destinationZone, departureTime);

        if (potentialTrips.isEmpty()) {
            addRStoBacklog(new UserDestinationZone(usersRepository.getUserByUserId(userId), destinationZone, departureTime));
            return;
        }

        Trip trip = potentialTrips.getFirst();
        if (trip.getUsers() == null) {
            trip.setUsers(new ArrayList<>());
        }
        trip.addUser(usersRepository.getUserByUserId(userId));
        tripRepository.save(trip);
    }
    /**
     * Checks if either the origin or destination lies inside the USIU perimeter.
     * Path2D treats X as Longitude and Y as Latitude.
     */
    private boolean checkForUsiuDestinationOrigin(OriginDestination originDestination) {
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

    public void addRStoBacklog(UserDestinationZone user){
        if (user == null || user.getUser() == null || user.getDestinationZone() == null || user.getPreferredDepartureTime() == null) {
            throw new IllegalArgumentException("Backlog entry is incomplete");
        }
        rideSeekerBacklog.add(user);
    }

    public User getRSfromBacklog(LocalDateTime dateTime, String destinationZone){
        for (UserDestinationZone userDestinationZone : rideSeekerBacklog) {
            boolean sameZone = Objects.equals(destinationZone, userDestinationZone.getDestinationZone());
            boolean sameDeparture = Objects.equals(dateTime, userDestinationZone.getPreferredDepartureTime());
            if (sameZone && sameDeparture && rideSeekerBacklog.remove(userDestinationZone)) {
                return userDestinationZone.getUser();
            }
        }
        return null;
    }

    private void onboardBackloggedRideSeekers(Trip trip) {
        while (trip.getTripCapacity() > 0) {
            User backloggedUser = getRSfromBacklog(trip.getDepartureTime(), trip.getDestinationZone());
            if (backloggedUser == null) {
                break;
            }
            trip.addUser(backloggedUser);
        }
    }
}
