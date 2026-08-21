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
        logger.info("Starting getRoute request for origin=[{}, {}] to destination=[{}, {}]",
                originDestination.originLatitude(),
                originDestination.originLongitude(),
                originDestination.destinationLatitude(),
                originDestination.destinationLongitude());

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
            logger.info("Google Routes API endpoint: {}", routesEndpoint);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("X-Goog-Api-Key", key);
            httpURLConnection.setRequestProperty("X-Goog-FieldMask", "routes.polyline.encodedPolyline");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.getOutputStream().write(outBoundJson.getBytes());
            httpURLConnection.getOutputStream().flush();
            httpURLConnection.getOutputStream().close();

                        int responseCode = httpURLConnection.getResponseCode();
            logger.info("Google Routes API response code: {}, message: {}", responseCode, httpURLConnection.getResponseMessage());

            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.error("Google Routes API returned non-OK status code: {}", responseCode);
                throw new RuntimeException("Google Routes API request failed with status code: " + responseCode);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(httpURLConnection.getInputStream());
            logger.debug("Full Google Routes API response JSON before field extraction: {}", root.toPrettyString());

            JsonNode routesNode = root.path("routes");
            if (!routesNode.isArray() || routesNode.size() == 0) {
                logger.error("No routes found in Google Routes API response");
                throw new RuntimeException("No routes found in Google Routes API response");
            }

            JsonNode polylineNode = routesNode.path(0).path("polyline").path("encodedPolyline");

            if (polylineNode.isMissingNode() || polylineNode.isNull()) {
                logger.error("Encoded polyline is missing or null in Google Routes API response");
                throw new RuntimeException("Encoded polyline not found in Google Routes API response");
            }

            String encodedPolyline = polylineNode.asText();

            if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
                logger.info("Successfully retrieved encoded polyline from Google Routes API: {}", encodedPolyline);
                return encodedPolyline;
            } else {
                logger.warn("Encoded polyline is empty in response from Google Routes API");
                throw new RuntimeException("Encoded polyline is empty");
            }

        } catch (Exception e) {
            logger.error("Error occurred while fetching route from Google Routes API: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getNeighborhoodZone(Double latitude, Double longitude) {
        logger.info("Starting getNeighborhoodZone request for latitude={}, longitude={}", latitude, longitude);

        String outBoundRequest = mapsEndpoint
                + "latlng="
                + latitude
                + ","
                + longitude
                + "&key="
                + key;

        try {
            logger.info("Google Maps Geocoding API endpoint: {}", mapsEndpoint);
            URL url = new URL(outBoundRequest);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            int responseCode = httpURLConnection.getResponseCode();
            logger.info("Google Maps Geocoding API response code: {}, message: {}", responseCode, httpURLConnection.getResponseMessage());

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
                        logger.debug("Found {} results from geocoding API", resultsNode.size());
                        for (JsonNode result : resultsNode) {
                            JsonNode addressComponents = result.path("address_components");

                            if (addressComponents.isArray()) {
                                for (JsonNode component : addressComponents) {
                                    JsonNode types = component.path("types");

                                    if (types.isArray()) {
                                        for (JsonNode type : types) {
                                            if ("neighborhood".equals(type.asText())) {
                                                String neighborhood = component.path("long_name").asText();
                                                logger.info("Successfully found neighborhood: {} for latitude={}, longitude={}",
                                                    neighborhood, latitude, longitude);
                                                return neighborhood;
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
