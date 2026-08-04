package org.hamisi.swoopdserver.in_app_messeging;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppTripMessagingServiceTests {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private FirebaseMessagingService firebaseMessagingService;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private InAppTripMessagingService inAppTripMessagingService;

    @Test
    @DisplayName("Broadcast message includes trip creator and excludes sender")
    void broadcastMessageIncludesTripCreatorAndExcludesSender() {
        UUID senderId = UUID.randomUUID();
        UUID otherPassengerId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        when(tripRepository.getUserIdsFromOpenTripWithUserId(senderId))
                .thenReturn(List.of(senderId, otherPassengerId));
        when(tripRepository.getCreatorIdsFromOpenTripWithUserId(senderId))
                .thenReturn(List.of(creatorId));
        when(usersRepository.getFullNameByUserId(senderId)).thenReturn("Passenger");

        inAppTripMessagingService.broadcastMessage(senderId, "Hello trip");

        verify(firebaseMessagingService, never()).sendData(eq(senderId), any(ChatMessageDto.class));
        verify(firebaseMessagingService, times(1)).sendData(eq(otherPassengerId), any(ChatMessageDto.class));
        verify(firebaseMessagingService, times(1)).sendData(eq(creatorId), any(ChatMessageDto.class));
    }
}

