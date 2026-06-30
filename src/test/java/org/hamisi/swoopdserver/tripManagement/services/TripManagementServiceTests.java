package org.hamisi.swoopdserver.tripManagement.services;

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

        when(googleRoutesProxy.getDestinationZone(request.destinationLatitude(), request.destinationLongitude()))
                .thenReturn("THIKA_ROAD");
        when(tripRepository.getTripsByTripStatusDestinationZonedTime(TripStatus.OPEN, "THIKA_ROAD", departureTime))
                .thenReturn(List.of());
        when(usersRepository.getUserByUserId(userId)).thenReturn(seeker);

        tripManagementService.joinCarpool(userId, departureTime, request);

        verify(tripRepository, never()).save(any(Trip.class));
        User firstMatch = tripManagementService.getRideSeekerFromBacklogHelper(departureTime, "THIKA_ROAD");
        User secondMatch = tripManagementService.getRideSeekerFromBacklogHelper(departureTime, "THIKA_ROAD");

        assertNotNull(firstMatch);
        assertEquals(userId, firstMatch.getUserId());
        assertNull(secondMatch);
    }

    @Test
    @DisplayName("Cancelling an open trip backlogs all affected passengers")
    void cancelTripBacklogsAffectedPassengers() {
        UUID hostId = UUID.randomUUID();

        User passengerOne = createUser(UUID.randomUUID());
        User passengerTwo = createUser(UUID.randomUUID());

        Trip openTrip = new Trip();
        openTrip.setTripStatus(TripStatus.OPEN);
        openTrip.setDestinationZone("WESTLANDS");
        openTrip.setDepartureTime(departureTime);
        openTrip.setUsers(new ArrayList<>(List.of(passengerOne, passengerTwo)));

        when(tripRepository.getTripByCreatedBy(hostId)).thenReturn(openTrip);

        tripManagementService.cancelTrip(hostId);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        assertEquals(TripStatus.CANCELLED, tripCaptor.getValue().getTripStatus());

        User first = tripManagementService.getRideSeekerFromBacklogHelper(departureTime, "WESTLANDS");
        User second = tripManagementService.getRideSeekerFromBacklogHelper(departureTime, "WESTLANDS");
        Set<UUID> backloggedUserIds = new HashSet<>(List.of(first.getUserId(), second.getUserId()));

        assertEquals(Set.of(passengerOne.getUserId(), passengerTwo.getUserId()), backloggedUserIds);
        assertNull(tripManagementService.getRideSeekerFromBacklogHelper(departureTime, "WESTLANDS"));
    }

    @Test
    @DisplayName("Create trip onboards matching users from backlog")
    void createTripOnboardsMatchingUsersFromBacklog() {
        UUID hostId = UUID.randomUUID();
        User host = createUser(hostId);
        User backloggedUser = createUser(UUID.randomUUID());

        OriginDestination route = new OriginDestination(36.879000, -1.215100, 36.900000, -1.200000);

        when(usersRepository.getUserByUserId(hostId)).thenReturn(host);
        when(googleRoutesProxy.getDestinationZone(route.destinationLatitude(), route.destinationLongitude()))
                .thenReturn("CBD");
        when(googleRoutesProxy.getRoute(route)).thenReturn("encoded-polyline");

        tripManagementService.addRStoBacklogHelper(new UserDestinationZone(backloggedUser, "CBD", departureTime));

        tripManagementService.createTrip(
                hostId,
                2,
                departureTime,
                route
        );

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));

        Trip savedTrip = tripCaptor.getValue();
        assertEquals(1, savedTrip.getUsers().size());
        assertEquals(backloggedUser.getUserId(), savedTrip.getUsers().getFirst().getUserId());
        assertEquals(1, savedTrip.getTripCapacity());
        assertEquals(TripStatus.OPEN, savedTrip.getTripStatus());
    }

    private User createUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }
}

