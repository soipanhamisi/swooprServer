package org.hamisi.swoopdserver.tripManagement.services;

import lombok.extern.slf4j.Slf4j;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.RideRequest;
import org.hamisi.swoopdserver.tripManagement.dtos.CommuteHistoryDto;
import org.hamisi.swoopdserver.tripManagement.dtos.TripData;
import org.hamisi.swoopdserver.tripManagement.dtos.TripInfo;
import org.hamisi.swoopdserver.tripManagement.dtos.TripUpdateNotification;
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

@Slf4j
@Service
public class TripManagementService {
    private static final String TRIP_MANAGEMENT_SOURCE = "TripManagementService";
    private static final String BACKLOG_EXPIRED_EVENT = "CARPOOL_MATCH_FAILED";
    private static final String BACKLOG_EXPIRED_MESSAGE = "We could not find a suitable carpool before your selected departure time. Please request again for a later time.";

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

    public void registerVehicle(UUID userId, VehicleDto vehicleDto) {
        String normalizedRegNo = normalizeRegNumber(vehicleDto.getRegNo());
        if (!verifyCarDetails(normalizedRegNo)) {
            throw new RegisterVehicleException("wrong number plate format");
        }
        if (vehicleRepository.existsByVehicleRegNumber(normalizedRegNo)) {
            throw new RegisterVehicleException("Vehicle already registered");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleRegNumber(normalizedRegNo);
        vehicle.setVehicleDescription(vehicleDto.getDesc());
        vehicle.setUser(usersRepository.getReferenceById(userId));
        vehicleRepository.save(vehicle);
    }

    private boolean verifyCarDetails(String replace) {
        if (replace == null) {
            return false;
        }

        String normalized = replace.strip().toLowerCase(Locale.ROOT);
        return normalized.matches("^k[a-z]{2}\\d{3}[a-z]$");
    }


    @Transactional
    public TripInfo createTrip(
            UUID userId,
            int tripCapacity,
            LocalDateTime departureTime,
            VehicleDto vehicle,
            OriginDestination originDestination
    ) {
        expireStaleBacklogEntries();
        if (tripRepository.belongsToAnOpenCarPool(userId)) {
            throw new CannotCreateTripException("Already in a carpool");
        }
        log.debug("trip is in ride host not in an open trip chek passed");
        if (tripCapacity <= 0) {
            throw new CannotCreateTripException("Trip capacity must be at least 1");
        }
        log.debug("trip passed minimum capacity check");
        if (originDestination == null) {
            throw new CannotCreateTripException("Origin and destination coordinates are required");
        }
        log.debug("origin destination fields filled");
        validateCoordinates(originDestination);
        if (vehicle == null || vehicle.getRegNo() == null || vehicle.getRegNo().isBlank()) {
            throw new CannotCreateTripException("Vehicle registration number is required");
        }
        log.debug("null vehicle field check passed");
        if (!usiuCampusGeofenceService.involvesUsiuCampus(originDestination)) {
            throw new CannotCreateTripException("Cannot create trips not involving the USIU campus");
        }
        log.debug("Geofence check passed");
        String normalizedRegNo = normalizeRegNumber(vehicle.getRegNo());
        Vehicle hostVehicle = vehicleRepository.getAllByUser_UserId(userId).stream()
                .filter(v -> normalizedRegNo.equals(normalizeRegNumber(v.getVehicleRegNumber())))
                .findFirst()
                .orElseThrow(() -> new CannotCreateTripException("Vehicle not registered to this user"));
        log.debug("Registered vehicle check passed");
        User host = usersRepository.getUserByUserId(userId);
        if (host == null) {
            throw new CannotCreateTripException("User not found");
        }
        log.debug("user exists check");

        String originZone = resolveZone(
                originDestination.originLatitude(),
                originDestination.originLongitude(),
                "Trip creation is temporarily unavailable. Please try again shortly."
        );
        log.debug("origin zone resolved");
        String destinationZone = resolveZone(
                originDestination.destinationLatitude(),
                originDestination.destinationLongitude(),
                "Trip creation is temporarily unavailable. Please try again shortly."
        );
        log.debug("destination zone resolved");
        String routePolyline = resolveRoutePolyline(originDestination);
        log.debug("polyline obtained");
        Trip trip = new Trip();
        trip.setVehicle(hostVehicle);
        trip.setTripCapacity(tripCapacity);
        trip.setUsers(new ArrayList<>());
        trip.addHost(host);
        trip.setDepartureTime(departureTime);
        trip.setOriginDestination(originDestination);
        trip.setCreatedBy(userId);
        trip.setTripStatus(TripStatus.OPEN);
        trip.setOriginZone(originZone);
        trip.setDestinationZone(destinationZone);
        trip.setRoutePolyline(routePolyline);
        onboardBackloggedRideSeekersHelper(trip);
        tripRepository.save(trip);
        log.debug("Trip record created in db");
        updateTripUsers(trip);
        log.debug("Trip onboarded users notified");

        return getTripInfo(userId);
    }

    private String normalizeRegNumber(String regNo) {
        if (regNo == null) {
            return "";
        }
        return regNo.strip().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    @Transactional
    public void cancelTrip(UUID userId) {
        List<Trip> trips = tripRepository.getOpenTrips(userId);
        if (trips == null || trips.isEmpty()) {
            throw new CannotCancelTripException("cannot cancel trip");
        }
        Trip trip = trips.get(0);
        if (trip == null || trip.getTripStatus() != TripStatus.OPEN) {
            throw new CannotCancelTripException("cannot cancel trip");
        }

        if (trip.getUsers() != null && !trip.getUsers().isEmpty()) {
            String cancellationMsg = "Your trip has been cancelled by carpool host." +
                    " You have been placed in a backlog and will be notified if another trip is available";
            for (User user : trip.getUsers().stream()
                    .filter(user -> user != null && !userId.equals(user.getUserId()))
                    .toList()) {
                firebaseMessagingService.sendNotification(
                        user.getUserId(),
                        TRIP_MANAGEMENT_SOURCE,
                        "TRIP_CANCELLED",
                        Map.of("message", cancellationMsg)
                );
                addRStoBacklogHelper(
                        user,
                        trip.getOriginZone(),
                        trip.getDestinationZone(),
                        LocalDateTime.now(),
                        trip.getDepartureTime()
                );
            }
        }

        trip.setTripStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

    @Transactional(noRollbackFor = NoAvailableTripException.class)
    public TripInfo joinCarpool(UUID userId,
                                LocalDateTime departureTime,
                                OriginDestination rsDestination) {
        expireStaleBacklogEntries();
        if (!usiuCampusGeofenceService.involvesUsiuCampus(rsDestination)) {
            throw new CannotCreateCarpoolRequestException("you must be going to or leaving the USIU premises");
        }
        if (tripRepository.belongsToAnOpenCarPool(userId) || rideSeekerBacklogRepository.isInBackLog(userId, LocalDateTime.now()))
            throw new CannotCreateTripException("Already in a carpool/Request already made");
        User seeker = usersRepository.getUserByUserId(userId);
        if (seeker == null) {
            throw new CannotCreateCarpoolRequestException("User not found");
        }
        validateCoordinates(rsDestination);
        String originZone = resolveZone(
                rsDestination.originLatitude(),
                rsDestination.originLongitude(),
                "Trip matching is temporarily unavailable. Please try again shortly."
        );
        String destinationZone = resolveZone(
                rsDestination.destinationLatitude(),
                rsDestination.destinationLongitude(),
                "Trip matching is temporarily unavailable. Please try again shortly."
        );
        List<Trip> potentialTrips = tripRepository.getTripsByTripStatusDestinationZonedTime(TripStatus.OPEN,
                destinationZone,
                departureTime);
        potentialTrips = potentialTrips == null ? List.of() : potentialTrips.stream()
                .filter(trip -> tripHasCompatibleOrigin(trip, originZone))
                .toList();
        if (potentialTrips.isEmpty()) {
            addRStoBacklogHelper(seeker, originZone, destinationZone, LocalDateTime.now(), departureTime);
            throw new NoAvailableTripException("There are no open trips currently. " +
                    "You will be notified if a new trip is available");
        }

        Trip trip = potentialTrips.getFirst();
        if (trip.getUsers() == null) {
            trip.setUsers(new ArrayList<>());
        }
        trip.addUser(seeker);
        tripRepository.save(trip);

        updateTripUsers(trip);
        return getTripInfo(userId);
    }


    public TripInfo getTripInfo(UUID userid) {
        Trip rawTrip = tripRepository.getOpenTripsWithUserId(userid);
        if (rawTrip == null) {
            throw new TripInfoException("not currently in any trip");
        }

        TripInfo tripInfo = new TripInfo();
        tripInfo.setTripData(new TripData());
        tripInfo.setCarpoolMemberNames(new ArrayList<>());

        tripInfo.getTripData().setCapacity(rawTrip.getTripCapacity());
        tripInfo.getTripData().setDepartureTime(rawTrip.getDepartureTime());
        tripInfo.getTripData().setOriginDestinationCoordinates(rawTrip.getOriginDestination());
        if (rawTrip.getUsers() != null) {
            for (User user : rawTrip.getUsers()) {
                if (user != null) {
                    tripInfo.getCarpoolMemberNames().add(user.getFullName());
                }
            }
        }
        return tripInfo;
    }

    public RideRequest getRideRequests(UUID userId) {
        expireStaleBacklogEntries();
        RideSeekerBacklogEntry rideSeekerBacklogEntry = rideSeekerBacklogRepository
                .getUserBacklogEntry(userId, LocalDateTime.now());
        if (rideSeekerBacklogEntry == null) {
            throw new NoRideRequestFoundException("No pending ride request found for user.");
        }
        return new RideRequest(
                rideSeekerBacklogEntry.getDestinationZone(),
                rideSeekerBacklogEntry.getRequestMadeAt()
        );
    }

    @Transactional
    public void cancelRideRequest(UUID userId) {
        RideSeekerBacklogEntry rideSeekerBacklogEntry = rideSeekerBacklogRepository
                .getUserBacklogEntry(userId, LocalDateTime.now());
        if (rideSeekerBacklogEntry == null) {
            throw new NoRideRequestFoundException("No pending ride request found for user.");
        }

        rideSeekerBacklogRepository.delete(rideSeekerBacklogEntry);
    }

    public List<VehicleDto> getRegisteredVehicles(UUID userId) {
        List<Vehicle> vehicles = vehicleRepository.getAllByUser_UserId(userId);
        List<VehicleDto> vehicleDto = new ArrayList<>();
        for (Vehicle v : vehicles) {
            VehicleDto dto = new VehicleDto();
            dto.setRegNo(v.getVehicleRegNumber());
            dto.setDesc(v.getVehicleDescription());
            vehicleDto.add(dto);
        }
        return vehicleDto;
    }

    public List<CommuteHistoryDto> getCommuteHistory(UUID userId) {
        List<Trip> pastTrips = tripRepository.getCommuteHistoryByUserId(userId);
        if (pastTrips.isEmpty()) {
            return List.of();
        }

        List<CommuteHistoryDto> commuteHistory = new ArrayList<>();
        for (Trip trip : pastTrips) {
            CommuteHistoryDto dto = new CommuteHistoryDto();
            dto.setOriginDestinationCoordinates(trip.getOriginDestination());
            dto.setDepartureDate(trip.getDepartureTime().toLocalDate());
            dto.setDepartureTime(trip.getDepartureTime().toLocalTime());
            commuteHistory.add(dto);
        }
        return commuteHistory;
    }

    private void updateTripUsers(Trip trip) {
        if (trip.getUsers() == null || trip.getUsers().isEmpty()) {
            return;
        }

        TripUpdateNotification payload = new TripUpdateNotification();
        payload.setTripCapacity(trip.getTripCapacity());
        payload.setDepartureTime(trip.getDepartureTime());
        payload.setOriginDestination(trip.getOriginDestination());
        payload.setTripStatus(trip.getTripStatus());
        payload.setDestinationZone(trip.getDestinationZone());
        payload.setRoutePolyline(trip.getRoutePolyline());
        payload.setCarpoolMemberNames(
                trip.getUsers().stream()
                        .filter(user -> user != null && user.getFullName() != null)
                        .map(User::getFullName)
                        .toList()
        );

        for (User user : trip.getUsers().stream().filter(user -> user != null && user.getUserId() != null).toList()) {
            firebaseMessagingService.sendNotification(user.getUserId(),
                    "Trip Management Service",
                    "TRIP_UPDATES",
                    payload);
        }
    }


    private void onboardBackloggedRideSeekersHelper(Trip trip) {
        expireStaleBacklogEntries();
        int availableSeats = trip.getTripCapacity();
        if (availableSeats <= 0) {
            return;
        }

        List<RideSeekerBacklogEntry> pendingEntries = rideSeekerBacklogRepository.findByMatchedFalseOrderByRequestMadeAtAsc();
        if (pendingEntries == null || pendingEntries.isEmpty()) {
            return;
        }

        List<RideSeekerBacklogEntry> matchedEntries = pendingEntries
                .stream()
                .filter(entry -> zonesAreCompatible(entry.getOriginZone(), trip.getOriginZone()))
                .filter(entry -> zonesAreCompatible(entry.getDestinationZone(), trip.getDestinationZone()))
                .limit(availableSeats)
                .toList();

        for (RideSeekerBacklogEntry matchedEntry : matchedEntries) {
            trip.addUser(matchedEntry.getUser());
            markBacklogEntryMatched(matchedEntry);
        }
    }

    private void addRStoBacklogHelper(User user,
                                      String originZone,
                                      String destinationZone,
                                      LocalDateTime requestMadeAt,
                                      LocalDateTime selectedDepartureTime) {
        if (user == null
                || originZone == null || originZone.isBlank()
                || destinationZone == null || destinationZone.isBlank()
                || requestMadeAt == null) {
            throw new IllegalArgumentException("Backlog entry is incomplete");
        }

        RideSeekerBacklogEntry backlogEntry = new RideSeekerBacklogEntry();
        backlogEntry.setUser(user);
        backlogEntry.setOriginZone(originZone);
        backlogEntry.setDestinationZone(destinationZone);
        backlogEntry.setRequestMadeAt(requestMadeAt);
        backlogEntry.setSelectedDepartureTime(selectedDepartureTime);
        backlogEntry.setMatched(false);
        backlogEntry.setMatchedAt(null);
        rideSeekerBacklogRepository.save(backlogEntry);
    }

    @Transactional
    public int expireStaleBacklogEntries() {
        LocalDateTime now = LocalDateTime.now();
        List<RideSeekerBacklogEntry> expiredEntries = rideSeekerBacklogRepository
                .findByMatchedFalseAndSelectedDepartureTimeBefore(now);
        if (expiredEntries == null || expiredEntries.isEmpty()) {
            return 0;
        }

        for (RideSeekerBacklogEntry expiredEntry : expiredEntries) {
            firebaseMessagingService.sendNotification(
                    expiredEntry.getUser().getUserId(),
                    TRIP_MANAGEMENT_SOURCE,
                    BACKLOG_EXPIRED_EVENT,
                    Map.of("message", BACKLOG_EXPIRED_MESSAGE)
            );
        }

        rideSeekerBacklogRepository.deleteAll(expiredEntries);
        return expiredEntries.size();
    }

    private void markBacklogEntryMatched(RideSeekerBacklogEntry backlogEntry) {
        backlogEntry.setMatched(true);
        backlogEntry.setMatchedAt(LocalDateTime.now());
        rideSeekerBacklogRepository.save(backlogEntry);
    }

    private boolean zonesAreCompatible(String firstZone, String secondZone) {
        String normalizedFirstZone = normalizeZone(firstZone);
        String normalizedSecondZone = normalizeZone(secondZone);

        if (normalizedFirstZone.isBlank() || normalizedSecondZone.isBlank()) {
            return false;
        }

        return normalizedFirstZone.contains(normalizedSecondZone)
                || normalizedSecondZone.contains(normalizedFirstZone);
    }

    private boolean tripHasCompatibleOrigin(Trip trip, String riderOriginZone) {
        if (trip == null) {
            return false;
        }

        return zonesAreCompatible(resolveTripOriginZone(trip), riderOriginZone);
    }

    private String resolveTripOriginZone(Trip trip) {
        if (trip.getOriginZone() != null && !trip.getOriginZone().isBlank()) {
            return trip.getOriginZone();
        }
        if (trip.getOriginDestination() == null) {
            return "";
        }
        return resolveZone(
                trip.getOriginDestination().originLatitude(),
                trip.getOriginDestination().originLongitude(),
                "Trip matching is temporarily unavailable. Please try again shortly."
        );
    }

    private String resolveZone(Double latitude, Double longitude, String failureMessage) {
        if (latitude == null || longitude == null) {
            throw new CannotCreateTripException("Origin and destination coordinates are required");
        }

        try {
            String zone = googleRoutesProxy.getDestinationZone(latitude, longitude);
            if (zone == null || zone.isBlank() || "Neighborhood Not Found".equalsIgnoreCase(zone)) {
                throw new IllegalStateException("Zone could not be resolved");
            }
            return zone;
        } catch (RuntimeException ex) {
            throw new GoogleMapsServiceUnavailableException(failureMessage, ex);
        }
    }

    private String resolveRoutePolyline(OriginDestination originDestination) {
        try {
            String routePolyline = googleRoutesProxy.getRoute(originDestination);
            if (routePolyline == null || routePolyline.isBlank()) {
                throw new IllegalStateException("Route polyline could not be resolved");
            }
            return routePolyline;
        } catch (RuntimeException ex) {
            throw new GoogleMapsServiceUnavailableException(
                    "Trip creation is temporarily unavailable. Please try again shortly.",
                    ex
            );
        }
    }

    private void validateCoordinates(OriginDestination originDestination) {
        if (originDestination.originLongitude() == null
                || originDestination.originLatitude() == null
                || originDestination.destinationLongitude() == null
                || originDestination.destinationLatitude() == null) {
            throw new CannotCreateTripException("Origin and destination coordinates are required");
        }
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