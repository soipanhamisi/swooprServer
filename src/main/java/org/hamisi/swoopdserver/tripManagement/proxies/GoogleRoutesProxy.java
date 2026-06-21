package org.hamisi.swoopdserver.tripManagement.proxies;


import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class GoogleRoutesProxy {
    @Value("${GOOGLE_ROUTES_ENDPOINT}")
    private String routesEndpoint;

    @Value("${GOOGLE_ROUTES_API_KEY}")
    private String key;

    public String getRoute(OriginDestination originDestination) {

        String outBoundJson = String.format(
                """
                        {"origin":{
                          "location": {
                          "latLng":{
                              "latitude": %f,
                              "longitude": %f
                          }
                          }
                        },
                        "destination": {
                          "location": {
                          "latLng":{
                              "latitude": %f,
                              "longitude": %f
                          }
                          }
                        }
                        }
                        """, originDestination.originLatitude(),
                originDestination.originLongitude(),
                originDestination.destinationLatitude(),
                originDestination.destinationLongitude());
        try {
            URL url = new URL(routesEndpoint);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("X-Goog-Api-Key", key);
            httpURLConnection.setRequestProperty("X-Goog-FieldMask", "routes.polyline.encodedPolyline");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.getOutputStream().write(outBoundJson.getBytes());
            httpURLConnection.getOutputStream().flush();
            httpURLConnection.getOutputStream().close();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(httpURLConnection.getInputStream());
            System.out.println("Response Code: " + httpURLConnection.getResponseCode());
            return root.path("routes").path("0").path("polyline").path("encodedPolyline").toString();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }
}
