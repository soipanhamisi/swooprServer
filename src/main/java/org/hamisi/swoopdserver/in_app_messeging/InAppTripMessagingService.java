package org.hamisi.swoopdserver.in_app_messeging;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InAppTripMessagingService {

    private final TripRepository tripRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final UsersRepository usersRepository;

    public InAppTripMessagingService(TripRepository tripRepository, FirebaseMessagingService firebaseMessagingService, UsersRepository usersRepository) {
        this.tripRepository = tripRepository;
        this.firebaseMessagingService = firebaseMessagingService;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public void broadcastMessage(UUID userId, String message) {
        Set<UUID> userIds = new LinkedHashSet<>(tripRepository.getUserIdsFromOpenTripWithUserId(userId));
        userIds.addAll(tripRepository.getCreatorIdsFromOpenTripWithUserId(userId));
        if (userIds.isEmpty()){
            return;
        }
        LocalDateTime timeStamp = LocalDateTime.now();
        String name  = usersRepository.getFullNameByUserId(userId);

        ChatMessageDto chatMessageDto = new ChatMessageDto(
                timeStamp,
                name,
                message
        );
        for (UUID id: userIds){
            if (id.equals(userId)){
                continue;
            }
            firebaseMessagingService.sendData(
                    id,
                    chatMessageDto
            );
        }
    }

    @Transactional
    public void broadcastMessageTest(String message) {
        List<UUID> userIds = usersRepository.getAllUserIds();
        if (userIds.isEmpty()){
            return;
        }
        for (UUID id: userIds){
            firebaseMessagingService.sendNotification(
                    id,
                    "test",
                    "Message",
                    message
            );
        }
    }
}
