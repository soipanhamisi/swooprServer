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

    @Value("${GCP_PROJECT_ID}")
    private String projectId;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseProxy.class);
    private final OAuthTokenProvider tokenProvider;

    public FirebaseProxy(OAuthTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void sendNotification(String msgToken, String message){
        String accessToken;
        accessToken = tokenProvider.getAccessToken();
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
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://fcm.googleapis.com/v1/projects/"
                                + projectId
                                + "/messages:send"
                ))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(outBoundJson))
                .build();
        HttpResponse<String> response;
        try {
            response = HttpClient.newHttpClient().send(httpRequest,  HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        if(response.statusCode()!=200){
            logger.error(response.body());
            throw  new RuntimeException();
        }
    }
}
