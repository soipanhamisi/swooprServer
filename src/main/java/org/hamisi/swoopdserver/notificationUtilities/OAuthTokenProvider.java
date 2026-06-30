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
            googleCredentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/firebase.messaging");
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException();
        }
    }

    public synchronized String getAccessToken(){
        try {
            googleCredentials.refreshIfExpired();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException();
        }
        return googleCredentials.getAccessToken().getTokenValue();
    }
}
