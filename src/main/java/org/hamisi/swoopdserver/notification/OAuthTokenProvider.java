package org.hamisi.swoopdserver.notification;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthTokenProvider {
    private final GoogleCredentials googleCredentials;

    public OAuthTokenProvider() throws IOException {
        googleCredentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/firebase.messaging");
    }

    public synchronized String getAccessToken() throws IOException{
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}
