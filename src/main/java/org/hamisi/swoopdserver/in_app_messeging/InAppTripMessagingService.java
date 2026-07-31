package org.hamisi.swoopdserver.in_app_messeging;

import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InAppTripMessagingService {

    private final TripRepository tripRepository;
    private final FirebaseMessagingService firebaseMessagingService;

    public InAppTripMessagingService(TripRepository tripRepository, FirebaseMessagingService firebaseMessagingService) {
        this.tripRepository = tripRepository;
        this.firebaseMessagingService = firebaseMessagingService;
    }


    public void broadcastMessage(UUID userId, String message) {
        List<UUID> userIds = tripRepository.getUserIdsFromOpenTripWithUserId(userId);
        String originService = "InAppTripMessagingService";
        for (UUID id: userIds){
            firebaseMessagingService.sendNotification(
                    id,
                    originService,
                    "Trip Message",
                    message
            );
        }
    }

}
