package org.hamisi.swoopdserver.auth.proxies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResendProxy {
    private final ObjectMapper objectMapper;

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;
    @Value("${EMAIL_TEMPLATE_ID}")
    private String templateId;
    @Value("${EMAIL_URL}")
    private String emailUrl;
    @Value("${RESEND_FROM_EMAIL:Swoopr <noreply@swoopr-authentication.soipan.rocks>}")
    private String resendFromEmail;

    public ResendProxy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendOTP(String email, String firstName, int otp) {
        Map<String, Object> outboundPayload = new LinkedHashMap<>();
        outboundPayload.put("from", resendFromEmail);
        outboundPayload.put("to", List.of(email));
        outboundPayload.put("subject", "Welcome to Swoopr carpool management");
        outboundPayload.put(
                "template",
                Map.of(
                        "id", templateId,
                        "variables", Map.of(
                                "first_name", firstName,
                                "otp_code", otp
                        )
                )
        );

        postEmail(outboundPayload, "Failed to send OTP email");
    }

    public void sendEmail(String email, String subject, String htmlBody, String textBody) {
        Map<String, Object> outboundPayload = new LinkedHashMap<>();
        outboundPayload.put("from", resendFromEmail);
        outboundPayload.put("to", List.of(email));
        outboundPayload.put("subject", subject);
        if (htmlBody != null && !htmlBody.isBlank()) {
            outboundPayload.put("html", htmlBody);
        }
        if (textBody != null && !textBody.isBlank()) {
            outboundPayload.put("text", textBody);
        }

        postEmail(outboundPayload, "Failed to send email");
    }

    private void postEmail(Map<String, Object> outboundPayload, String failureMessage) {
        String outboundJson = serialize(outboundPayload);
        try {
            URL url = new URL(emailUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + resendApiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(outboundJson.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                String errorResponse = readInputStream(connection.getErrorStream());
                throw new IllegalStateException(failureMessage + ". HTTP status: " + statusCode + " Response: " + errorResponse);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private String serialize(Map<String, Object> outboundPayload) {
        try {
            return objectMapper.writeValueAsString(outboundPayload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize email payload", exception);
        }
    }

    private String readInputStream(InputStream input) throws IOException {
        if (input == null) return "";
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}