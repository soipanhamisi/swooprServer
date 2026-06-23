package org.hamisi.swoopdserver.tripManagement.proxies;


import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

@Component
public class GoogleRoutesProxy {
    @Value("${GOOGLE_ROUTES_ENDPOINT}")
    private String routesEndpoint;

    @Value("${GOOGLE_ROUTES_API_KEY}")
    private String key;

    @Value("${GOOGLE_MAPS_ENDPOINT}")
    private String mapsEndpoint;

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

    public String getDestinationZone(Double latitude, Double longitude) {
        String outBoundRequest = mapsEndpoint
                + "latlng="
                + latitude.toString()
                + ","
                + longitude.toString()
                + "&key="
                + key;

        try {
            URL url = new URL(outBoundRequest);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response body
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse JSON using Jackson
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.toString());
                JsonNode resultsNode = rootNode.path("results");

                // Loop through all results dynamically
                if (resultsNode.isArray()) {
                    for (JsonNode result : resultsNode) {
                        JsonNode addressComponents = result.path("address_components");

                        if (addressComponents.isArray()) {
                            for (JsonNode component : addressComponents) {
                                JsonNode types = component.path("types");

                                // Check if this component types array contains "neighborhood"
                                if (types.isArray()) {
                                    for (JsonNode type : types) {
                                        if ("neighborhood".equals(type.asText())) {
                                            return component.path("long_name").asText(); // Returns "Thome"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fallback if no specific neighborhood type was found in the data
                return "Neighborhood Not Found";

            } else {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }

        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
