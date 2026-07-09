package org.hamisi.swoopdserver.notificationUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class FirebaseProxy {

    @Value("${FIREBASE_PROJECT_ID}")
    private String projectId;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseProxy.class);
    private final OAuthTokenProvider tokenProvider;

    public FirebaseProxy(OAuthTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void sendNotification(String msgToken, String message){
        logger.info("Initiating Firebase Cloud Messaging notification - token: {}, message length: {}",
                msgToken, message.length());

        if (projectId == null || projectId.isBlank()) {
            logger.error("Firebase configuration error: GCP_PROJECT_ID environment variable is not set. " +
                    "Please set GCP_PROJECT_ID to your Firebase project ID.");
            throw new RuntimeException("GCP_PROJECT_ID is not configured");
        }
        logger.debug("Using GCP Project ID: {}", projectId);

        String accessToken;
        try {
            accessToken = tokenProvider.getAccessToken();
            logger.debug("Successfully obtained Firebase OAuth access token");
        } catch (RuntimeException e) {
            logger.error("Failed to obtain Firebase OAuth access token. This typically indicates an authentication issue. " +
                    "Ensure Google Application Default Credentials are properly configured. Error: {}",
                    e.getMessage(), e);
            throw e;
        }

        String outBoundJson = """
        {
            "message": {
                "token": "%s",
                "notification": {
                    "title": "%s",
                    "body": "%s"
                }
            }
        }
        """.formatted(msgToken, "Notification", message);
        logger.debug("Firebase notification payload: {}", outBoundJson);

        String firebaseUrl = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";
        logger.debug("Firebase API endpoint: {}", firebaseUrl);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(firebaseUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(outBoundJson))
                .build();

        HttpResponse<String> response;
        try {
            logger.info("Sending Firebase notification request to FCM API");
            response = HttpClient.newHttpClient().send(httpRequest,  HttpResponse.BodyHandlers.ofString());
            logger.debug("Firebase API response status: {}", response.statusCode());
        } catch (IOException e) {
            logger.error("Network error while communicating with Firebase Cloud Messaging API. " +
                    "Check your internet connection and Firebase service availability. " +
                    "IOException: {}", e.getMessage(), e);
            throw new RuntimeException("Network error communicating with Firebase: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Request to Firebase Cloud Messaging API was interrupted. " +
                    "InterruptedException: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firebase request was interrupted: " + e.getMessage(), e);
        }

        if(response.statusCode() != 200){
            String errorBody = response.body();
            logger.error("Firebase Cloud Messaging API returned error status code: {}. " +
                    "Response body: {}. This indicates a misconfiguration or invalid Firebase token. " +
                    "Verify: 1) GCP_PROJECT_ID is correct, 2) Firebase token is valid, " +
                    "3) Firebase service account has proper permissions",
                    response.statusCode(), errorBody);
            throw new RuntimeException("Firebase notification failed with status " + response.statusCode() +
                    ": " + errorBody);
        }

        logger.info("Firebase notification sent successfully");
    }
}
