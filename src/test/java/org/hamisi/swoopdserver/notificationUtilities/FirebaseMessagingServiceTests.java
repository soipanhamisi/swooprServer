package org.hamisi.swoopdserver.notificationUtilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseMessagingServiceTests {

    @Mock
    private MessagingTokenRepository messagingTokenRepository;

    @Mock
    private FirebaseProxy firebaseProxy;

    private FirebaseMessagingService firebaseMessagingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        firebaseMessagingService = new FirebaseMessagingService(messagingTokenRepository, firebaseProxy, objectMapper);
    }

    @Test
    @DisplayName("sendNotification builds outbound JSON and forwards it to FirebaseProxy")
    void sendNotificationBuildsOutboundJsonAndForwardsToProxy() {
        UUID userId = UUID.randomUUID();
        when(messagingTokenRepository.getMessagingByTokenUserId(userId)).thenReturn("  sample-token  ");

        Map<String, Object> payload = Map.of(
                "tripId", "trip-123",
                "status", "OPEN"
        );

        firebaseMessagingService.sendNotification(userId, "TripService", "TRIP_UPDATE", payload);

        ArgumentCaptor<String> outboundJsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(firebaseProxy).sendNotification(outboundJsonCaptor.capture());

        String outboundJson = outboundJsonCaptor.getValue();
        assertTrue(outboundJson.contains("\"token\": \"sample-token\""));
        assertTrue(outboundJson.contains("\"originService\": \"TripService\""));
        assertTrue(outboundJson.contains("\"notificationType\": \"TRIP_UPDATE\""));
        assertTrue(outboundJson.contains("\"payload\": \""));
        assertTrue(outboundJson.contains("tripId\\\":\\\"trip-123\\\""));
    }

    @Test
    @DisplayName("sendNotification returns early when no messaging token exists")
    void sendNotificationReturnsEarlyWhenTokenMissing() {
        UUID userId = UUID.randomUUID();
        when(messagingTokenRepository.getMessagingByTokenUserId(userId)).thenReturn("   ");

        firebaseMessagingService.sendNotification(userId, "TripService", "TRIP_UPDATE", Map.of("tripId", "trip-123"));

        verify(firebaseProxy, never()).sendNotification(anyString());
    }

    @Test
    @DisplayName("sendNotification wraps repository failures")
    void sendNotificationWrapsRepositoryFailures() {
        UUID userId = UUID.randomUUID();
        when(messagingTokenRepository.getMessagingByTokenUserId(userId)).thenThrow(new RuntimeException("db down"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> firebaseMessagingService.sendNotification(userId, "TripService", "TRIP_UPDATE", Map.of("tripId", "trip-123"))
        );

        assertTrue(exception.getMessage().contains("Failed to retrieve messaging token for user"));
        verify(firebaseProxy, never()).sendNotification(anyString());
    }

    @Test
    @DisplayName("sendNotification serializes arbitrary object payloads")
    void sendNotificationSerializesArbitraryObjectPayloads() {
        UUID userId = UUID.randomUUID();
        when(messagingTokenRepository.getMessagingByTokenUserId(userId)).thenReturn("sample-token");

        TripPayload payload = new TripPayload("trip-123", 2);

        firebaseMessagingService.sendNotification(userId, "TripService", "TRIP_UPDATE", payload);

        ArgumentCaptor<String> outboundJsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(firebaseProxy).sendNotification(outboundJsonCaptor.capture());

        String outboundJson = outboundJsonCaptor.getValue();
        assertTrue(outboundJson.contains("trip-123"));
        assertTrue(outboundJson.contains("2"));
    }

    private record TripPayload(String tripId, int seatsRemaining) {
    }
}

