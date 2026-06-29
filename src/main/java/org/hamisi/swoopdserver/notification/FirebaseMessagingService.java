package org.hamisi.swoopdserver.notification;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    public void sendNotification(UUID userId, String message) throws IOException, InterruptedException {
        String msgToken = messagingTokenRepository.getMessagingByTokenUserId(userId);
        firebaseProxy.sendNotification(msgToken, message);
    }
}
