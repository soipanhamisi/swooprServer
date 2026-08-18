package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.dtos.VehicleDto;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.entities.Vehicle;
import org.hamisi.swoopdserver.tripManagement.geofence.UsiuCampusGeofenceService;
import org.hamisi.swoopdserver.tripManagement.proxies.GoogleRoutesProxy;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.tripManagement.repositories.VehicleRepository;
import org.hamisi.swoopdserver.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TripLifecycleManagementServiceTests {

    @Mock
    FirebaseMessagingService firebaseMessagingService;
    @Mock
    VehicleRepository vehicleRepository;
    @Mock
    UsiuCampusGeofenceService usiuCampusGeofenceService;
    @Mock
    TripRepository tripRepository;
    @Mock
    GoogleRoutesProxy googleRoutesProxy;
    @Mock
    UsersRepository usersRepository;

    @InjectMocks
    TripLifecycleManagementService service;

    private UUID userid;
    private LocalDateTime departureTime;
    private VehicleDto vehicleDto;
    private OriginDestination od;

    @BeforeEach
    void setUp() {
        userid = UUID.randomUUID();
        departureTime = LocalDateTime.now().plusHours(2);
        vehicleDto = new VehicleDto();
        vehicleDto.setRegNo("KCS966G");
        vehicleDto.setDesc("Subaru Forester");
        od = new OriginDestination(
                -1.218,
                36.878,
                -1.252,
                36.889);
    }

    @Nested
    @DisplayName("createTrip")
    class CreateTripTests {

        @Test
        @DisplayName("Returns early when user already has an active trip")
        void returnsEarlyWhenUserHasActiveTrip() {
            when(tripRepository.belongsToAnOpenCarPool(userid)).thenReturn(true);

            service.createTrip(userid, 3, departureTime, vehicleDto, od);

            verify(tripRepository, never()).save(any());
            verify(firebaseMessagingService).sendNotification(
                    eq(userid), eq("TRIP_MANAGEMENT"), eq("TRIP_CREATION"), any()
            );
        }

        @Test
        @DisplayName("Creates trip on valid input")
        void createsTripOnValidInput() {
            when(tripRepository.belongsToAnOpenCarPool(userid)).thenReturn(false);
            Vehicle vehicle = new Vehicle();

            vehicle.setVehicleRegNumber(vehicleDto.getRegNo());
            vehicle.setVehicleDescription(vehicleDto.getDesc());
            when(vehicleRepository.findVehicleByUser_UserId(userid)).thenReturn(List.of(vehicle));

            when(usiuCampusGeofenceService.involvesUsiuCampus(od)).thenReturn(true);

            when(googleRoutesProxy.getDestinationZone(anyDouble(), anyDouble())).thenReturn("Kasarani", "Karen");

            when(googleRoutesProxy.getRoute(od)).thenReturn("encodedPolyline");

            User host = new User();
            host.setUserId(userid);
            when(usersRepository.getReferenceById(userid)).thenReturn(host);

            Trip savedTrip = new Trip();
            savedTrip.setTripId(UUID.randomUUID());
            when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);

            service.createTrip(
                    userid,
                    4,
                    departureTime,
                    vehicleDto,
                    od
            );

            verify(tripRepository).save(any(Trip.class));

            verify(firebaseMessagingService, atLeastOnce()).sendNotification(
                    eq(userid),
                    eq("TRIP_MANAGEMENT"),
                    eq("TRIP_CREATION"),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("cancelTrip")
    class cancelTripTests {
        @Test
        @DisplayName("Cancels Trip given valid inputs")
        void cancelTripOnValidInput() {
            // arrange
            Trip trip = new Trip();
            trip.setTripId(UUID.randomUUID());
            trip.setTripStatus(TripStatus.OPEN);
            trip.setCreatedBy(userid);

            User host = new User();
            host.setUserId(userid);

            User rider1 = new User();
            rider1.setUserId(UUID.randomUUID());

            User rider2 = new User();
            rider2.setUserId(UUID.randomUUID());

            trip.addUser(host);
            trip.addUser(rider1);
            trip.addUser(rider2);

            when(tripRepository.getReferenceById(trip.getTripId())).thenReturn(trip);

            // act
            service.cancelTrip(userid, trip.getTripId());

            // assert
            verify(tripRepository).save(trip);

            verify(firebaseMessagingService, atLeastOnce()).sendNotification(
                    eq(userid),
                    eq("TRIP_MANAGEMENT"),
                    eq("TRIP_CANCELLATION"),
                    any()
            );

            // make sure the trip status was updated
            assert trip.getTripStatus() == TripStatus.CANCELLED;
        }

        @Test
        @DisplayName("Trip can only be canceled by the creator")
        void notCreatedBy() {
            Trip trip = new Trip();
            trip.setTripId(UUID.randomUUID());
            trip.setTripStatus(TripStatus.OPEN);
            trip.setCreatedBy(UUID.randomUUID());

            User rider1 = new User();
            rider1.setUserId(UUID.randomUUID());

            User rider2 = new User();
            rider2.setUserId(UUID.randomUUID());
            trip.addUser(rider1);
            trip.addUser(rider2);

            when(tripRepository.getReferenceById(trip.getTripId())).thenReturn(trip);

            service.cancelTrip(userid, trip.getTripId());

            verify(tripRepository, never()).save(any(Trip.class));
            verify(firebaseMessagingService).sendNotification(
                    eq(userid),
                    eq("TRIP_MANAGEMENT"),
                    eq("TRIP_CANCELLATION"),
                    any()
            );
        }
    }
}
