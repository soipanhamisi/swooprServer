package org.hamisi.swoopdserver.notificationUtilities;

import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirebaseMessagingService {

    private final MessagingTokenRepository messagingTokenRepository;
    private final FirebaseProxy firebaseProxy;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseMessagingService.class);
    private final ObjectMapper jacksonJsonMapper;

    public FirebaseMessagingService(MessagingTokenRepository messagingTokenRepository,
                                    FirebaseProxy firebaseProxy,
                                    ObjectMapper jacksonJsonMapper) {
        this.messagingTokenRepository = messagingTokenRepository;
        this.jacksonJsonMapper = jacksonJsonMapper;
        this.firebaseProxy = firebaseProxy;

    }

    @Transactional
    public <T> void sendNotification(UUID userId, String originService, String notificationType, T payload) {
        logger.info("Attempting to send Firebase notification to user: {}", userId);

        String msgToken = retrieveMessagingToken(userId);
        if (msgToken == null) {
            return;
        }

        try {
            String outBoundJson = buildOutboundJson(msgToken, originService, notificationType, payload);
            firebaseProxy.sendNotification(outBoundJson);
            logger.info("Firebase notification sent successfully to user: {}", userId);
        } catch (RuntimeException e) {
            logger.error("Failed to send Firebase notification to user: {}. " +
                            "Error: {}. This may indicate authentication or configuration issues.",
                    userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves the Firebase messaging token for a given user.
     *
     * @param userId The UUID of the user
     * @return The messaging token, or null if not found
     * @throws RuntimeException if database error occurs
     */
    private String retrieveMessagingToken(UUID userId) {
        logger.debug("Retrieving Firebase messaging token for user: {}", userId);
        try {
            String msgToken = messagingTokenRepository.getMessagingByTokenUserId(userId);
            if (msgToken == null || msgToken.isBlank()) {
                logger.warn("No Firebase messaging token found for user: {}. " +
                        "User may not have registered a device for notifications.", userId);
                return null;
            }
            msgToken = msgToken.trim();
            logger.debug("Retrieved Firebase messaging token for user: {}, token length: {}", userId, msgToken.length());
            return msgToken;
        } catch (Exception e) {
            logger.error("Failed to retrieve Firebase messaging token for user: {}. " +
                    "Database error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve messaging token for user: " + userId, e);
        }
    }

    /**
     * Constructs the pre-formatted FCM JSON payload with metadata and serialized payload.
     *
     * @param msgToken The Firebase messaging token
     * @param originService The origin service identifier
     * @param notificationType The type of notification
     * @param payload The payload object to be serialized
     * @return Pre-formatted FCM JSON string
     * @throws RuntimeException if payload serialization fails
     */
    private <T> String buildOutboundJson(String msgToken, String originService, String notificationType, T payload) {
        logger.debug("Building outbound JSON payload for token: {}, service: {}, type: {}",
                msgToken, originService, notificationType);

        String serializedPayload = serializePayload(payload);
        String escapedPayload = escapeJsonString(serializedPayload);

        String outBoundJson = """
                {
                    "message": {
                        "token": "%s",
                        "data": {
                            "originService": "%s",
                            "notificationType": "%s",
                            "payload": "%s"
                        }
                    }
                }
                """.formatted(msgToken, originService, notificationType, escapedPayload);

        logger.debug("Outbound JSON constructed successfully");
        return outBoundJson;
    }

    /**
     * Serializes an object to a JSON string using Jackson ObjectMapper.
     *
     * @param payload The object to serialize
     * @return JSON string representation
     * @throws RuntimeException if serialization fails
     */
    private <T> String serializePayload(T payload) {
        logger.debug("Serializing payload object of type: {}", payload.getClass().getSimpleName());
        try {
            String serialized = jacksonJsonMapper.writeValueAsString(payload);
            logger.debug("Payload serialized successfully, length: {}", serialized.length());
            return serialized;
        } catch (Exception e) {
            logger.error("Failed to serialize payload object. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize notification payload: " + e.getMessage(), e);
        }
    }

    /**
     * Escapes special characters in a JSON string to safely embed it in another JSON structure.
     * Escapes backslashes and double quotes.
     *
     * @param jsonString The JSON string to escape
     * @return Escaped JSON string
     */
    private String escapeJsonString(String jsonString) {
        logger.debug("Escaping JSON string for safe embedding");
        return jsonString
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}