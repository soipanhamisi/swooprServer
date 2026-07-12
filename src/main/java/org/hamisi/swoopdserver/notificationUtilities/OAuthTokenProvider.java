package org.hamisi.swoopdserver.notificationUtilities;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthTokenProvider {
    private final GoogleCredentials googleCredentials;
    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenProvider.class);

    public OAuthTokenProvider(){
        try {
            logger.info("Initializing Google OAuth credentials for Firebase Cloud Messaging");
            googleCredentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/firebase.messaging");
            logger.info("Successfully initialized Google OAuth credentials with Firebase Messaging scope");
        } catch (IOException e) {
            logger.error("Failed to initialize Google OAuth credentials. This typically means: " +
                    "1) Google Application Default Credentials are not configured, " +
                    "2) GOOGLE_APPLICATION_CREDENTIALS environment variable is not set, " +
                    "3) Service account JSON file is missing or invalid. " +
                    "To fix: Set GOOGLE_APPLICATION_CREDENTIALS to point to your service account JSON file. " +
                    "IOException: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase authentication: " + e.getMessage(), e);
        }
    }

    public synchronized String getAccessToken(){
        try {
            logger.debug("Refreshing Google OAuth access token");
            googleCredentials.refreshIfExpired();
            String token = googleCredentials.getAccessToken().getTokenValue();
            logger.debug("Firebase access token obtained successfully, token length: {}", token.length());
            return token;
        } catch (IOException e) {
            logger.error("Failed to refresh/obtain Firebase access token. This may indicate: " +
                    "1) Service account credentials are invalid, " +
                    "2) Network connectivity issue with Google OAuth endpoint, " +
                    "3) Service account lacks required permissions. " +
                    "IOException: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to obtain Firebase access token: " + e.getMessage(), e);
        } catch (NullPointerException e) {
            logger.error("Null pointer while retrieving Firebase access token. This indicates Google credentials " +
                    "were not properly initialized. Check your GOOGLE_APPLICATION_CREDENTIALS setting. " +
                    "NullPointerException: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase credentials not properly initialized", e);
        }
    }
}
