package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripManagementServiceTests {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private GoogleRoutesProxy googleRoutesProxy;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private RideSeekerBacklogRepository rideSeekerBacklogRepository;

    @Mock
    private UsiuCampusGeofenceService usiuCampusGeofenceService;

    @Mock
    private FirebaseMessagingService firebaseMessagingService;

    @InjectMocks
    private TripManagementService tripManagementService;

    private LocalDateTime departureTime;

    @BeforeEach
    void setUp() {
        departureTime = LocalDateTime.of(2026, 7, 1, 8, 0);
    }

    @Test
    @DisplayName("Join carpool adds user to backlog when no trip is available")
    void joinCarpoolAddsUserToBacklogWhenNoTripFound() {
        UUID userId = UUID.randomUUID();
        User seeker = createUser(userId);
        OriginDestination request = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);
        LocalDateTime beforeRequest = LocalDateTime.now();

        when(googleRoutesProxy.getDestinationZone(request.destinationLatitude(), request.destinationLongitude()))
                .thenReturn("THIKA_ROAD");
        when(usiuCampusGeofenceService.involvesUsiuCampus(request)).thenReturn(true);
        when(tripRepository.getTripsByTripStatusDestinationZonedTime(TripStatus.OPEN, "THIKA_ROAD", departureTime))
                .thenReturn(List.of());
        when(usersRepository.getUserByUserId(userId)).thenReturn(seeker);

        NoAvailableTripException exception = assertThrows(
                NoAvailableTripException.class,
                () -> tripManagementService.joinCarpool(userId, departureTime, request)
        );
        LocalDateTime afterRequest = LocalDateTime.now();

        verify(tripRepository, never()).save(any(Trip.class));
        ArgumentCaptor<RideSeekerBacklogEntry> backlogCaptor = ArgumentCaptor.forClass(RideSeekerBacklogEntry.class);
        verify(rideSeekerBacklogRepository, times(1)).save(backlogCaptor.capture());

        RideSeekerBacklogEntry savedBacklogEntry = backlogCaptor.getValue();

        assertEquals("There are no open trips currently. You will be notified if a new trip is available", exception.getMessage());
        assertEquals(userId, savedBacklogEntry.getUser().getUserId());
        assertEquals("THIKA_ROAD", savedBacklogEntry.getDestinationZone());
        assertFalse(savedBacklogEntry.isMatched());
        assertNull(savedBacklogEntry.getMatchedAt());
        assertNotNull(savedBacklogEntry.getRequestMadeAt());
        assertFalse(savedBacklogEntry.getRequestMadeAt().isBefore(beforeRequest));
        assertFalse(savedBacklogEntry.getRequestMadeAt().isAfter(afterRequest));
    }


    @Test
    @DisplayName("Cancelling an open trip backlogs all affected passengers")
    void cancelTripBacklogsAffectedPassengers() {
        UUID hostId = UUID.randomUUID();
        LocalDateTime beforeCancellation = LocalDateTime.now();

        User passengerOne = createUser(UUID.randomUUID());
        User passengerTwo = createUser(UUID.randomUUID());

        Trip openTrip = new Trip();
        openTrip.setTripId(UUID.randomUUID()); // Added missing tripId
        openTrip.setTripStatus(TripStatus.OPEN);
        openTrip.setDestinationZone("WESTLANDS");
        openTrip.setDepartureTime(departureTime);
        openTrip.setUsers(new ArrayList<>(List.of(passengerOne, passengerTwo)));

        when(tripRepository.getTripByCreatedBy(hostId)).thenReturn(openTrip);

        tripManagementService.cancelTrip(hostId);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        verify(firebaseMessagingService, times(1))
                .sendNotification(
                        passengerOne.getUserId(),
                        "TripManagementService",
                        "TRIP_CANCELLED",
                        Map.of("message", "Your trip has been cancelled by carpool host. You have been placed in a backlog and will be notified if another trip is available")
                );
        verify(firebaseMessagingService, times(1))
                .sendNotification(
                        passengerTwo.getUserId(),
                        "TripManagementService",
                        "TRIP_CANCELLED",
                        Map.of("message", "Your trip has been cancelled by carpool host. You have been placed in a backlog and will be notified if another trip is available")
                );
        assertEquals(TripStatus.CANCELLED, tripCaptor.getValue().getTripStatus());
        LocalDateTime afterCancellation = LocalDateTime.now();

        ArgumentCaptor<RideSeekerBacklogEntry> backlogCaptor = ArgumentCaptor.forClass(RideSeekerBacklogEntry.class);
        verify(rideSeekerBacklogRepository, times(2)).save(backlogCaptor.capture());
        Set<UUID> backloggedUserIds = new HashSet<>();

        for (RideSeekerBacklogEntry backlogEntry : backlogCaptor.getAllValues()) {
            backloggedUserIds.add(backlogEntry.getUser().getUserId());
            assertEquals("WESTLANDS", backlogEntry.getDestinationZone());
            assertFalse(backlogEntry.isMatched());
            assertNull(backlogEntry.getMatchedAt());
            assertNotNull(backlogEntry.getRequestMadeAt());
            assertFalse(backlogEntry.getRequestMadeAt().isBefore(beforeCancellation));
            assertFalse(backlogEntry.getRequestMadeAt().isAfter(afterCancellation));
        }

        assertEquals(Set.of(passengerOne.getUserId(), passengerTwo.getUserId()), backloggedUserIds);
    }

    @Test
    @DisplayName("Join carpool request fails when the ride-seeker destination doesn't involve USIU campus")
    void joinCarpoolFailsWhenDestinationDoesNotInvolveUsiuCampus() {
        UUID userId = UUID.randomUUID();
        OriginDestination request = new OriginDestination(36.879000, -1.215100, 36.882000, -1.214000);

        when(usiuCampusGeofenceService.involvesUsiuCampus(request)).thenReturn(false);

        CannotCreateCarpoolRequestException exception = assertThrows(
                CannotCreateCarpoolRequestException.class,
                () -> tripManagementService.joinCarpool(userId, departureTime, request)
        );

        assertEquals("you must be going to or leaving the USIU premises", exception.getMessage());
        verify(googleRoutesProxy, never()).getDestinationZone(any(Double.class), any(Double.class));
        verify(tripRepository, never()).getTripsByTripStatusDestinationZonedTime(any(), any(), any());
        verify(tripRepository, never()).save(any(Trip.class));
        verify(rideSeekerBacklogRepository, never()).save(any(RideSeekerBacklogEntry.class));
    }

    @Test
    @DisplayName("Join carpool throws a service unavailable exception when Google Maps API fails")
    void joinCarpoolThrowsWhenGoogleMapsApiIsUnavailable() {
        UUID userId = UUID.randomUUID();
        OriginDestination request = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);

        when(usiuCampusGeofenceService.involvesUsiuCampus(request)).thenReturn(true);
        when(googleRoutesProxy.getDestinationZone(request.destinationLatitude(), request.destinationLongitude()))
                .thenThrow(new RuntimeException("Google Maps API unreachable"));

        GoogleMapsServiceUnavailableException exception = assertThrows(
                GoogleMapsServiceUnavailableException.class,
                () -> tripManagementService.joinCarpool(userId, departureTime, request)
        );

        assertEquals("Trip matching is temporarily unavailable. Please try again shortly.", exception.getMessage());
        verify(tripRepository, never()).save(any(Trip.class));
        verify(rideSeekerBacklogRepository, never()).save(any(RideSeekerBacklogEntry.class));
    }

    @Test
    @DisplayName("Cancel trip fails when no trip is found for the host")
    void cancelTripFailsWhenTripNotFound() {
        UUID hostId = UUID.randomUUID();

        when(tripRepository.getTripByCreatedBy(hostId)).thenReturn(null);

        CannotCancelTripException exception = assertThrows(
                CannotCancelTripException.class,
                () -> tripManagementService.cancelTrip(hostId)
        );

        assertEquals("cannot cancel trip", exception.getMessage());
        verify(tripRepository, never()).save(any(Trip.class));
        verify(rideSeekerBacklogRepository, never()).save(any(RideSeekerBacklogEntry.class));
    }

    @Test
    @DisplayName("Cancel trip fails when the trip is not in OPEN status")
    void cancelTripFailsWhenTripIsNotOpen() {
        UUID hostId = UUID.randomUUID();

        Trip cancelledTrip = new Trip();
        cancelledTrip.setTripStatus(TripStatus.CANCELLED);

        when(tripRepository.getTripByCreatedBy(hostId)).thenReturn(cancelledTrip);

        CannotCancelTripException exception = assertThrows(
                CannotCancelTripException.class,
                () -> tripManagementService.cancelTrip(hostId)
        );

        assertEquals("cannot cancel trip", exception.getMessage());
        verify(tripRepository, never()).save(any(Trip.class));
        verify(rideSeekerBacklogRepository, never()).save(any(RideSeekerBacklogEntry.class));
    }

    @Test
    @DisplayName("Create trip matches the oldest similar-zone backlog entries up to capacity and marks them as matched")
    void createTripOnboardsMatchingUsersFromPersistedBacklog() {
        UUID hostId = UUID.randomUUID();
        User oldestMatchingUser = createUser(UUID.randomUUID());
        User secondMatchingUser = createUser(UUID.randomUUID());
        User overflowMatchingUser = createUser(UUID.randomUUID());
        User differentZoneUser = createUser(UUID.randomUUID());

        OriginDestination route = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);
        Vehicle vehicle = new Vehicle();

        when(usiuCampusGeofenceService.involvesUsiuCampus(route)).thenReturn(true);
        when(vehicleRepository.findVehicleByUser_UserId(hostId)).thenReturn(vehicle);
        when(googleRoutesProxy.getDestinationZone(route.destinationLatitude(), route.destinationLongitude()))
                .thenReturn("THIKA_ROAD");
        when(googleRoutesProxy.getRoute(route)).thenReturn("encoded-polyline");
        when(rideSeekerBacklogRepository.findByMatchedFalseOrderByRequestMadeAtAsc()).thenReturn(List.of(
                createBacklogEntry(differentZoneUser, "WESTLANDS", departureTime.minusMinutes(30)),
                createBacklogEntry(oldestMatchingUser, "Thika Road", departureTime.minusMinutes(20)),
                createBacklogEntry(secondMatchingUser, "THIKA_ROAD", departureTime.minusMinutes(10)),
                createBacklogEntry(overflowMatchingUser, "thika-road", departureTime.minusMinutes(5))
        ));

        tripManagementService.createTrip(
                hostId,
                2,
                departureTime,
                route
        );

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        verify(firebaseMessagingService, times(1))
                .sendNotification(
                        oldestMatchingUser.getUserId(),
                        "TripManagementService",
                        "CARPOOL_MATCHED",
                        Map.of("message", "You have been matched to a new carpool")
                );
        verify(firebaseMessagingService, times(1))
                .sendNotification(
                        secondMatchingUser.getUserId(),
                        "TripManagementService",
                        "CARPOOL_MATCHED",
                        Map.of("message", "You have been matched to a new carpool")
                );
        verify(firebaseMessagingService, never())
                .sendNotification(
                        overflowMatchingUser.getUserId(),
                        "TripManagementService",
                        "CARPOOL_MATCHED",
                        Map.of("message", "You have been matched to a new carpool")
                );

        ArgumentCaptor<RideSeekerBacklogEntry> matchedBacklogCaptor = ArgumentCaptor.forClass(RideSeekerBacklogEntry.class);
        verify(rideSeekerBacklogRepository, times(2)).save(matchedBacklogCaptor.capture());
        Set<UUID> matchedBacklogUserIds = new HashSet<>();

        for (RideSeekerBacklogEntry backlogEntry : matchedBacklogCaptor.getAllValues()) {
            matchedBacklogUserIds.add(backlogEntry.getUser().getUserId());
            assertTrue(backlogEntry.isMatched());
            assertNotNull(backlogEntry.getMatchedAt());
        }

        Trip savedTrip = tripCaptor.getValue();
        assertEquals(Set.of(oldestMatchingUser.getUserId(), secondMatchingUser.getUserId()), matchedBacklogUserIds);
        assertEquals(2, savedTrip.getUsers().size());
        assertEquals(oldestMatchingUser.getUserId(), savedTrip.getUsers().get(0).getUserId());
        assertEquals(secondMatchingUser.getUserId(), savedTrip.getUsers().get(1).getUserId());
        assertEquals(0, savedTrip.getTripCapacity());
        assertEquals(TripStatus.FULL, savedTrip.getTripStatus());
        assertEquals(vehicle, savedTrip.getVehicle());
    }

    @Test
    @DisplayName("Create trip fails when route does not involve the USIU campus")
    void createTripFailsWhenRouteDoesNotInvolveUsiuCampus() {
        UUID hostId = UUID.randomUUID();
        OriginDestination route = new OriginDestination(36.820000, -1.280000, 36.900000, -1.200000);
        when(usiuCampusGeofenceService.involvesUsiuCampus(route)).thenReturn(false);

        CannotCreateTripException exception = assertThrows(
                CannotCreateTripException.class,
                () -> tripManagementService.createTrip(hostId, 2, departureTime, route)
        );

        assertEquals("Cannot create trips not involving the USIU campus", exception.getMessage());
        verify(vehicleRepository, never()).findVehicleByUser_UserId(any(UUID.class));
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    @DisplayName("Create trip fails when route coordinates are missing")
    void createTripFailsWhenRouteCoordinatesAreMissing() {
        UUID hostId = UUID.randomUUID();

        CannotCreateTripException exception = assertThrows(
                CannotCreateTripException.class,
                () -> tripManagementService.createTrip(hostId, 2, departureTime, null)
        );

        assertEquals("Origin and destination coordinates are required", exception.getMessage());
        verify(usiuCampusGeofenceService, never()).involvesUsiuCampus(any(OriginDestination.class));
        verify(vehicleRepository, never()).findVehicleByUser_UserId(any(UUID.class));
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    @DisplayName("Create trip fails when host has no registered vehicle")
    void createTripFailsWhenHostHasNoRegisteredVehicle() {
        UUID hostId = UUID.randomUUID();
        OriginDestination route = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);

        when(usiuCampusGeofenceService.involvesUsiuCampus(route)).thenReturn(true);
        when(vehicleRepository.findVehicleByUser_UserId(hostId)).thenReturn(null);

        CannotCreateTripException exception = assertThrows(
                CannotCreateTripException.class,
                () -> tripManagementService.createTrip(hostId, 2, departureTime, route)
        );

        assertEquals("No registered vehicle", exception.getMessage());
        verify(tripRepository, never()).save(any(Trip.class));
        verify(googleRoutesProxy, never()).getDestinationZone(any(Double.class), any(Double.class));
        verify(googleRoutesProxy, never()).getRoute(any(OriginDestination.class));
    }

    @Test
    @DisplayName("Create trip fails when host already has an open trip they created")
    void createTripFailsWhenHostAlreadyHasAnOpenTripTheyCreated() {
        UUID hostId = UUID.randomUUID();
        OriginDestination route = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);

        // Mock that the host does NOT belong to any other open carpool (as passenger)
        when(tripRepository.belongsToAnOpenCarPool(hostId)).thenReturn(true);

        CannotCreateTripException exception = assertThrows(
                CannotCreateTripException.class,
                () -> tripManagementService.createTrip(hostId, 2, departureTime, route)
        );

        assertEquals("Already in a carpool", exception.getMessage());
        verify(vehicleRepository, never()).findVehicleByUser_UserId(any(UUID.class));
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    @DisplayName("Create trip fails if user is already in an open carpool (as host or passenger)")
    void createTripFailsWhenUserIsAlreadyInOpenCarpool() {
        UUID userId = UUID.randomUUID(); // Renamed from hostId for clarity as it checks for any open carpool
        OriginDestination originDestination = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);

        when(tripRepository.belongsToAnOpenCarPool(userId)).thenReturn(true);

        assertThrows(
                CannotCreateTripException.class,
                () -> tripManagementService.createTrip(
                        userId,
                        4,
                        departureTime,
                        originDestination
                )
        );

        verify(tripRepository, never()).save(any(Trip.class));
    }

    private User createUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }

    private RideSeekerBacklogEntry createBacklogEntry(User user, String destinationZone, LocalDateTime requestMadeAt) {
        RideSeekerBacklogEntry backlogEntry = new RideSeekerBacklogEntry();
        backlogEntry.setBacklogEntryId(UUID.randomUUID());
        backlogEntry.setUser(user);
        backlogEntry.setDestinationZone(destinationZone);
        backlogEntry.setRequestMadeAt(requestMadeAt);
        backlogEntry.setMatched(false);
        backlogEntry.setMatchedAt(null);
        return backlogEntry;
    }
}