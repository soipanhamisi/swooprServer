package org.hamisi.swoopdserver.notificationUtilities;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirebaseMessagingService {

    private final MessagingTokenRepository messagingTokenRepository;
    private final FirebaseProxy firebaseProxy;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseMessagingService.class);

    public FirebaseMessagingService(MessagingTokenRepository messagingTokenRepository, FirebaseProxy firebaseProxy) {
        this.messagingTokenRepository = messagingTokenRepository;
        this.firebaseProxy = firebaseProxy;
    }

    @Transactional
    public void sendNotification(UUID userId, String message){
        logger.info("Attempting to send Firebase notification to user: {}", userId);

        String msgToken;
        try {
            msgToken = messagingTokenRepository.getMessagingByTokenUserId(userId);
            if (msgToken == null || msgToken.isBlank()) {
                logger.warn("No Firebase messaging token found for user: {}. " +
                        "User may not have registered a device for notifications.", userId);
                return;
            }
            logger.debug("Retrieved Firebase messaging token for user: {}, token length: {}", userId, msgToken.length());
        } catch (Exception e) {
            logger.error("Failed to retrieve Firebase messaging token for user: {}. " +
                    "Database error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve messaging token for user: " + userId, e);
        }

        try {
            firebaseProxy.sendNotification(msgToken, message);
            logger.info("Firebase notification sent successfully to user: {}", userId);
        } catch (RuntimeException e) {
            logger.error("Failed to send Firebase notification to user: {}. " +
                    "Error: {}. This may indicate authentication or configuration issues.",
                    userId, e.getMessage(), e);
            throw e;
        }
    }
}
