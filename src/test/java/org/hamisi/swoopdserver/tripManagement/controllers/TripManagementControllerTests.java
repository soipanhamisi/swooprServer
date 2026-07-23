package org.hamisi.swoopdserver.tripManagement.controllers;

import org.hamisi.swoopdserver.common.AccessRecord;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.tripManagement.dtos.TripData;
import org.hamisi.swoopdserver.tripManagement.dtos.TripInfo;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.services.GoogleMapsServiceUnavailableException;
import org.hamisi.swoopdserver.tripManagement.services.NoAvailableTripException;
import org.hamisi.swoopdserver.tripManagement.services.TripManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripManagementControllerTests {

    private static final String AUTH_HEADER = "Bearer test-token";

    private TripManagementService tripManagementService;
    private TokenManagementService tokenManagementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tripManagementService = mock(TripManagementService.class);
        tokenManagementService = mock(TokenManagementService.class);

        TripManagementController controller = new TripManagementController(tripManagementService, tokenManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TripManagementControllerExceptionHandlers())
                .build();
    }


    @ParameterizedTest
    @ValueSource(strings = {"/trips/joinCarpool", "/trips/joinCarPool"})
    @DisplayName("Google Maps outages return a stable 503 error body on both endpoint variants")
    void joinCarpoolReturnsServiceUnavailableWhenGoogleMapsIsDown(String endpoint) throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime departureTime = LocalDateTime.of(2026, 7, 11, 9, 0);
        String outageMessage = "Trip matching is temporarily unavailable. Please try again shortly.";

        when(tokenManagementService.verifyToken(AUTH_HEADER))
                .thenReturn(new AccessRecord(userId.toString(), "student@usiu.ac.ke"));
        when(tripManagementService.joinCarpool(eq(userId), eq(departureTime), any()))
                .thenThrow(new GoogleMapsServiceUnavailableException(outageMessage, new RuntimeException("maps down")));

        mockMvc.perform(post(endpoint)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildJoinCarpoolPayload(departureTime)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(outageMessage))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/trips/joinCarpool", "/trips/joinCarPool"})
    @DisplayName("No available trips returns accepted with structured backlog response")
    void joinCarpoolReturnsAcceptedWhenNoTripIsAvailable(String endpoint) throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime departureTime = LocalDateTime.of(2026, 7, 11, 9, 0);
        String backlogMessage = "There are no open trips currently. You will be notified if a new trip is available";

        when(tokenManagementService.verifyToken(AUTH_HEADER))
                .thenReturn(new AccessRecord(userId.toString(), "student@usiu.ac.ke"));
        when(tripManagementService.joinCarpool(eq(userId), eq(departureTime), any()))
                .thenThrow(new NoAvailableTripException(backlogMessage));

        mockMvc.perform(post(endpoint)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildJoinCarpoolPayload(departureTime)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(backlogMessage))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }


    private String buildJoinCarpoolPayload(LocalDateTime departureTime) {
        return """
                {
                  "departureTime": "%s",
                  "rsOriginDestination": {
                    "originLongitude": 36.8781,
                    "originLatitude": -1.2132,
                    "destinationLongitude": 36.9050,
                    "destinationLatitude": -1.2260
                  }
                }
                """.formatted(departureTime);
    }
}



