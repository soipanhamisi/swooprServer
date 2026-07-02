package org.hamisi.swoopdserver.notificationUtilities;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirebaseMessagingService {

    private final MessagingTokenRepository messagingTokenRepository;
    private final FirebaseProxy firebaseProxy;

    public FirebaseMessagingService(MessagingTokenRepository messagingTokenRepository, FirebaseProxy firebaseProxy) {
        this.messagingTokenRepository = messagingTokenRepository;
        this.firebaseProxy = firebaseProxy;
    }

    @Transactional
    public void sendNotification(UUID userId, String message){
        String msgToken = messagingTokenRepository.getMessagingByTokenUserId(userId);
        firebaseProxy.sendNotification(msgToken, message);
    }
}
