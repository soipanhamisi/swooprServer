package org.hamisi.swoopdserver.tripManagementTests.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.tripManagement.CannotCreateTripException;
import org.hamisi.swoopdserver.tripManagement.dtos.TripCreationDTO;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.proxies.GoogleRoutesProxy;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.tripManagement.services.TripManagementService;
import org.hamisi.swoopdserver.users.Role;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TripManagementServiceTests {
    @Mock
    private GoogleRoutesProxy googleRoutesProxy;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private TripManagementService tripManagementService;

    private TripCreationDTO tripCreationDTO;
    private User mockUser;
    private String encodedPolyline;

    @BeforeEach
    void setUp(){
        mockUser = new User();
        mockUser.setEmail("student@usiu.ac.ke");
        mockUser.setRole(Role.CARPOOL_HOST);

        encodedPolyline = "pj{Fywv_F{Ni^_@q@c@_@WCOKGQBUFQZOVAJDL\\@PEL`E`K~@~B|@zBxGn" +
                "Pj@rA`@x@B^DBJM`F}B@KNUtA_AdAm@nAo@rEgCfDuA`@Sf@S`Dy@xD{@pEcApCw@fGuB" +
                "lVgJrRcH`G{BlE}AxB}@VEjFuBpBcA|BsAhB}AbBaB|AoB`B_CfA_C@IdCwEzUaf@DW`A}BhFiLlAwBPs" +
                "@Z[xAoCbD{G?aAI[SU_@K[?[LSXGRB`@N\\\\`@`B`AbAp@bBxAdBpBxAvB|BtE`@dA~@~Cf@dCl@~ELdBfBt" +
                "P|BbSn@fHdAvI^|DVlB|@nEj@dBdA|BzBbEt@~AjA~Ct@tCVtBNrCCjDE~@QhBWfBw@nEQvAKnAEzA@vAN|BzBpTR" +
                "pDDrD@hB]fe@GjBK~AQ~Aa@xB_@vA{@dCiA|Bw@pA}AtBkEhF}IpLsG|JkBlCwAfBaA~@mCxBgAn@yEzCi@d@mDpBsIxF" +
                "wBxAeAv@eA~@}@`AkA~As@lAs@rB]bAi@jCY`DA~AF|BTzBT~ADj@~@~Fb@hEH~CA`DGxBWbDe@jCuAdH]vBMhDCrALrBVxBl" +
                "@dCx@vBfBzC|BjClC`CjBrBv@jAxAzC`@pA^bBV|BHjBFbHFzBFp@LdAl@pCZ`AxAbDnCzD~@pAj@`Af@bA`@dAj@vBTdARhB`AxKv" +
                "Dbf@p@hHLrC@r@AfAGvAY|BE\\c@`BsGxRoBhFyB|Gq@bB]r@mAfBaBjB_DnCcDhCsS|PeChBmBdAkAd@eBj@wKnBeCr@_Ab@i@\\" +
                "{@n@sAjAmBtByBhCu@dAWTy@hAiAnBu@zAqBfEyDrJkAdDqAlFy@nE{@tFsA`KaA`J}@lJe@pGaAvP_@zIMrAM`AQz@g@bBUn@w@`Bg@" +
                "v@{AlBoJrJ_CnCeCdDw@lA_BzCq@~Ae@rAcAjD_AfEa@dCeApI{ArNo@pEi@hCu@jCkA|CaBbDcBnCuAfBqBtBsAlAw@l@Kh@B^Ll@Ff@?`" +
                "@QdFkGy@uF_C]S_A]WS`Ae@R]f@a@n@Sl@Gn@@n@LrA`@TBbAEZQlCqBzAsApBuBdAoA`" +
                "AwAlAwBvAwCj@yAZaAt@oCf@kC`@uCjCcVl@gEv@eEf@oB^oAp@sBbA}BvAmC" +
                "hCwDhC_DfEmEjEkEdAoAX_@}@]Oj@Ws@e@[gAYqCk@`@}@fBqDvFz@";

        tripCreationDTO = new TripCreationDTO();
        tripCreationDTO.setEmail("student@usiu.ac.ke");
        tripCreationDTO.setTripCapacity(4);
        tripCreationDTO.setDepartureTime(LocalDateTime.now().plusHours(2));
        tripCreationDTO.setOriginLatitude((long) -1.2921);
        tripCreationDTO.setOriginLongitude((long) 36.8219);
        tripCreationDTO.setDestinationLatitude((long) -1.3032);
        tripCreationDTO.setDestinationLongitude((long) 36.7073);
    }

    @Test
    @DisplayName("Test to save trip with encoded polyline")
    void createTrip(){
        // Arrange
        when(usersRepository.findByEmail("student@usiu.ac.ke"))
                .thenReturn(Optional.of(mockUser));
        when(googleRoutesProxy.getRoute(
                tripCreationDTO.getDestinationLongitude(),
                tripCreationDTO.getDestinationLatitude(),
                tripCreationDTO.getOriginLongitude(),
                tripCreationDTO.getOriginLatitude()))
                .thenReturn(encodedPolyline);

        // Act
        tripManagementService.createTrip(tripCreationDTO);

        // Assert
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());

        Trip savedTrip = tripCaptor.getValue();
        assertEquals(4, savedTrip.getTripCapacity());
        assertEquals(TripStatus.OPEN, savedTrip.getTripStatus());
        assertEquals(tripCreationDTO.getDepartureTime(), savedTrip.getDepartureTime());
        assertEquals(encodedPolyline, savedTrip.getRoutePolyline());
    }
    @Test
    @DisplayName("Test to stop trip creation if user not carpool host")
    void createTripNotCarpoolHost(){
        mockUser.setRole(Role.RIDE_SEEKER);
        when(usersRepository.findByEmail("student@usiu.ac.ke"))
                .thenReturn(Optional.of(mockUser));

        CannotCreateTripException exception = assertThrows(CannotCreateTripException.class,
                () -> tripManagementService.createTrip(tripCreationDTO));

    }

    @Test
    @DisplayName("Test to delete trip")
    void cancelTrip(){
        when(usersRepository.findByEmail("student@usiu.ac.ke"))
                .thenReturn(Optional.of(mockUser));
        tripManagementService.cancelTrip("student@usiu.ac.ke");

        verify(tripRepository, times(1)).cancelTrip("student@usiu.ac.ke");

    }
}
