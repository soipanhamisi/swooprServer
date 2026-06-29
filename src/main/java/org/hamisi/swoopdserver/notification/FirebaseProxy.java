package org.hamisi.swoopdserver.notification;

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
    private final OAuthTokenProvider tokenProvider;

    public FirebaseProxy(OAuthTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void sendNotification(String msgToken, String message) throws IOException, InterruptedException {
        String accessToken = tokenProvider.getAccessToken();
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
        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest,  HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200){
            throw  new RuntimeException(response.body());
        }
    }
}
