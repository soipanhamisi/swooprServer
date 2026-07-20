package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.entities.Vehicle;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.proxies.GoogleRoutesProxy;
import org.hamisi.swoopdserver.tripManagement.repositories.RideSeekerBacklogRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.VehicleRepository;
import org.hamisi.swoopdserver.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TripManagementService {
    private final UsersRepository usersRepository;
    private final VehicleRepository vehicleRepository;
    private final GoogleRoutesProxy googleRoutesProxy;
    private final TripRepository tripRepository;
    private final RideSeekerBacklogRepository rideSeekerBacklogRepository;
    private final UsiuCampusGeofenceService usiuCampusGeofenceService;
    private final FirebaseMessagingService firebaseMessagingService;

    public TripManagementService(UsersRepository usersRepository,
                                 VehicleRepository vehicleRepository,
                                 GoogleRoutesProxy googleRoutesProxy,
                                 TripRepository tripRepository,
                                 RideSeekerBacklogRepository rideSeekerBacklogRepository,
                                 UsiuCampusGeofenceService usiuCampusGeofenceService,
                                 FirebaseMessagingService firebaseMessagingService) {
        this.usersRepository = usersRepository;
        this.vehicleRepository = vehicleRepository;
        this.googleRoutesProxy = googleRoutesProxy;
        this.tripRepository = tripRepository;
        this.rideSeekerBacklogRepository = rideSeekerBacklogRepository;
        this.usiuCampusGeofenceService = usiuCampusGeofenceService;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    public void registerVehicle(UUID userId, VehicleDto vehicleDto){
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleRegNumber(vehicleDto.getRegNo());
        vehicle.setVehicleDescription(vehicleDto.getDesc());
        vehicle.setUser(usersRepository.getReferenceById(userId));
        vehicleRepository.save(vehicle);
    }


    @Transactional
    public void createTrip(
            UUID userId,
            int tripCapacity,
            LocalDateTime departureTime,
            OriginDestination originDestination
    ){
        if (tripRepository.belongsToAnOpenCarPool(userId)){
            throw new CannotCreateTripException("Already in a carpool");
        }
        if (originDestination == null) {
            throw new CannotCreateTripException("Origin and destination coordinates are required");
        }

        if (!usiuCampusGeofenceService.involvesUsiuCampus(originDestination)){
            throw new CannotCreateTripException("Cannot create trips not involving the USIU campus");
        }
        if(tripRepository.getOpenTripsByCreatedByUserId(userId)){
            throw new CannotCreateTripException("Open pending trip present");
        }
        Vehicle hostVehicle = vehicleRepository.findVehicleByUser_UserId(userId);
        if (hostVehicle == null){
            throw new CannotCreateTripException("No registered vehicle");
        }
        Trip trip = new Trip();
        trip.setVehicle(hostVehicle);
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
        updateTripUsers(trip);
    }

    @Transactional
    public void cancelTrip(UUID userId)  {
        Trip trip = tripRepository.getTripByCreatedBy(userId);

        if(trip == null || trip.getTripStatus() != TripStatus.OPEN){
            throw new CannotCancelTripException("cannot cancel trip");
        }

        if (trip.getUsers() != null && !trip.getUsers().isEmpty()) {
            String cancellationMsg = "Your trip has been cancelled by carpool host." +
                    " You have been placed in a backlog and will be notified if another trip is available";
            for (User user : trip.getUsers()) {
                firebaseMessagingService.sendNotification(
                        user.getUserId(),
                        "TripManagementService",
                        "TRIP_CANCELLED",
                        Map.of("message", cancellationMsg)
                );
                addRStoBacklogHelper(user, trip.getDestinationZone(), LocalDateTime.now());
            }
        }

        trip.setTripStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

    @Transactional
    public Trip joinCarpool(UUID userId,
                            LocalDateTime departureTime,
                            OriginDestination rsDestination) {
        if (!usiuCampusGeofenceService.involvesUsiuCampus(rsDestination)){
            throw new CannotCreateCarpoolRequestException("you must be going to or leaving the USIU premises");
        }
        if (tripRepository.belongsToAnOpenCarPool(userId) || rideSeekerBacklogRepository.isInBackLog(userId))
            throw new CannotCreateTripException("Already in a carpool/Request already made");
        String destinationZone;
        try {
            destinationZone = googleRoutesProxy.getDestinationZone(rsDestination.destinationLatitude(),
                    rsDestination.destinationLongitude());
        } catch (RuntimeException ex) {
            throw new GoogleMapsServiceUnavailableException(
                    "Trip matching is temporarily unavailable. Please try again shortly.",
                    ex
            );
        }
        List<Trip> potentialTrips = tripRepository.getTripsByTripStatusDestinationZonedTime(TripStatus.OPEN, destinationZone, departureTime);
        if (potentialTrips.isEmpty()) {
            addRStoBacklogHelper(usersRepository.getUserByUserId(userId), destinationZone, LocalDateTime.now());
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
                    "TripManagementService",
                    "CARPOOL_JOINED",
                    Map.of("message", usersRepository.getFullNameByUserId(userId) + joinNotification)
            );
        }
        updateTripUsers(trip);
        return trip;

    }

    public List<VehicleDto> getRegisteredVehicles(UUID userId) {
        List<Vehicle> vehicles = vehicleRepository.getAllByUser_UserId(userId);
        List<VehicleDto> vehicleDto  = new ArrayList<>();
        for (Vehicle v : vehicles) {
            VehicleDto dto = new VehicleDto();
            dto.setRegNo(v.getVehicleRegNumber());
            dto.setDesc(v.getVehicleDescription());
            vehicleDto.add(dto);
        }
        return vehicleDto;
    }

    private void updateTripUsers(Trip trip){
        for (User user : trip.getUsers()){
            firebaseMessagingService.sendNotification(user.getUserId(),
                    "Trip Management Service",
                    "TRIP_UPDATES",
                    trip);
        }
    }


    private void onboardBackloggedRideSeekersHelper(Trip trip)  {
        int availableSeats = trip.getTripCapacity();
        if (availableSeats <= 0) {
            return;
        }

        List<RideSeekerBacklogEntry> matchedEntries = rideSeekerBacklogRepository.findByMatchedFalseOrderByRequestMadeAtAsc()
                .stream()
                .filter(entry -> destinationZonesAreSimilar(entry.getDestinationZone(), trip.getDestinationZone()))
                .limit(availableSeats)
                .toList();

        for (RideSeekerBacklogEntry matchedEntry : matchedEntries) {
            trip.addUser(matchedEntry.getUser());
            markBacklogEntryMatched(matchedEntry);
            firebaseMessagingService.sendNotification(
                        matchedEntry.getUser().getUserId(),
                        "TripManagementService",
                        "CARPOOL_MATCHED",
                        Map.of("message", "You have been matched to a new carpool")
                );
            }
        }
        private void addRStoBacklogHelper(User user, String destinationZone, LocalDateTime requestMadeAt) {
            if (user == null || destinationZone == null || destinationZone.isBlank() || requestMadeAt == null) {
                throw new IllegalArgumentException("Backlog entry is incomplete");
            }

            RideSeekerBacklogEntry backlogEntry = new RideSeekerBacklogEntry();
            backlogEntry.setUser(user);
            backlogEntry.setDestinationZone(destinationZone);
            backlogEntry.setRequestMadeAt(requestMadeAt);
            backlogEntry.setMatched(false);
            backlogEntry.setMatchedAt(null);
            rideSeekerBacklogRepository.save(backlogEntry);
        }

        private void markBacklogEntryMatched(RideSeekerBacklogEntry backlogEntry) {
            backlogEntry.setMatched(true);
            backlogEntry.setMatchedAt(LocalDateTime.now());
            rideSeekerBacklogRepository.save(backlogEntry);
        }

        private boolean destinationZonesAreSimilar(String firstZone, String secondZone) {
            String normalizedFirstZone = normalizeZone(firstZone);
            String normalizedSecondZone = normalizeZone(secondZone);

            if (normalizedFirstZone.isBlank() || normalizedSecondZone.isBlank()) {
                return false;
            }

            return normalizedFirstZone.contains(normalizedSecondZone)
                    || normalizedSecondZone.contains(normalizedFirstZone);
        }

        private String normalizeZone(String zone) {
            if (zone == null) {
                return "";
            }

            return zone
                    .strip()
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }
