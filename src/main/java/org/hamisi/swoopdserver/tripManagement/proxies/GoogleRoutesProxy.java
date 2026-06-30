package org.hamisi.swoopdserver.tripManagement.proxies;


import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class GoogleRoutesProxy {
    @Value("${GOOGLE_ROUTES_ENDPOINT}")
    private String routesEndpoint;

    @Value("${GOOGLE_ROUTES_API_KEY}")
    private String key;

    @Value("${GOOGLE_MAPS_ENDPOINT}")
    private String mapsEndpoint;

    private static final Logger logger = LoggerFactory.getLogger(GoogleRoutesProxy.class);

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
            logger.info(httpURLConnection.getResponseMessage());
            return root.path("routes").path("0").path("polyline").path("encodedPolyline").toString();

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public String getDestinationZone(Double latitude, Double longitude) {
        String outBoundRequest = mapsEndpoint
                + "latlng="
                + latitude
                + ","
                + longitude
                + "&key="
                + key;

        try {
            URL url = new URL(outBoundRequest);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(response.toString());
                    JsonNode resultsNode = rootNode.path("results");

                    if (resultsNode.isArray()) {
                        for (JsonNode result : resultsNode) {
                            JsonNode addressComponents = result.path("address_components");

                            if (addressComponents.isArray()) {
                                for (JsonNode component : addressComponents) {
                                    JsonNode types = component.path("types");

                                    if (types.isArray()) {
                                        for (JsonNode type : types) {
                                            if ("neighborhood".equals(type.asText())) {
                                                return component.path("long_name").asText();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    logger.warn("No neighborhood found for latitude={} longitude={}", latitude, longitude);
                    return "Neighborhood Not Found";
                }
            } else {
                logger.error(
                        "Google Maps geocoding request failed for latitude={} longitude={} with response code={} and message={}",
                        latitude,
                        longitude,
                        responseCode,
                        httpURLConnection.getResponseMessage()
                );
                throw new RuntimeException("Failed to fetch destination zone from Google Maps API");
            }

        } catch (Exception e) {
            logger.error(
                    "Error fetching destination zone for latitude={} longitude={}: {}",
                    latitude,
                    longitude,
                    e.getMessage(),
                    e
            );
            throw new RuntimeException("Failed to fetch destination zone", e);
        }
    }
}
