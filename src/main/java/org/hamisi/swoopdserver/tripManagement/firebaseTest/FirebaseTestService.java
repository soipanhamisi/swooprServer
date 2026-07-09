package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import org.hamisi.swoopdserver.notificationUtilities.FirebaseProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTestService {

    private final FirebaseProxy firebaseProxy;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseTestService.class);

    public FirebaseTestService(FirebaseProxy firebaseProxy) {
        this.firebaseProxy = firebaseProxy;
        logger.info("FirebaseTestService initialized for Firebase configuration testing");
    }

    public void sendNotification(String firebaseMessagingToken, String message) {
        logger.info("Firebase test service received notification request");

        if (firebaseMessagingToken == null || firebaseMessagingToken.isBlank()) {
            logger.warn("Validation failed: Firebase messaging token is null or blank");
            throw new IllegalArgumentException("Firebase messaging token is required");
        }

        if (message == null || message.isBlank()) {
            logger.warn("Validation failed: Message is null or blank");
            throw new IllegalArgumentException("Message is required");
        }

        String trimmedToken = firebaseMessagingToken.trim();
        String trimmedMessage = message.trim();

        logger.info("Passing notification to FirebaseProxy - token length: {}, message length: {}",
                trimmedToken.length(), trimmedMessage.length());

        try {
            firebaseProxy.sendNotification(trimmedToken, trimmedMessage);
            logger.info("Firebase test notification completed successfully");
        } catch (RuntimeException e) {
            logger.error("Firebase test notification failed. Error type: {}. Message: {}. " +
                    "This indicates a Firebase configuration or authentication issue.",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }
}

