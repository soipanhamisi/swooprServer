package org.hamisi.swoopdserver.auth.proxies;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class ResendProxy {
    @Value("${RESEND_API_KEY}")
    private String resendApiKey;
    @Value("${EMAIL_TEMPLATE_ID}")
    private String templateId;
    @Value("${EMAIL_URL}")
    private String emailUrl;

public void sendOTP(String email, String firstName, int otp) {
    String outboundJson = String.format("""
            {"from":"Swoopr <noreply@swoopr-authentication.soipan.rocks>","to":["%s"],"subject":"Welcome to Swoopr carpool management","template":{"id":"%s","variables":{"first_name":"%s","otp_code":%d}}}
            """, email, templateId, firstName, otp).trim();

    System.out.println("DEBUG: JSON being sent: " + outboundJson);
    System.out.println("DEBUG: JSON length: " + outboundJson.length());
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
            System.err.println("Resend API Error: " + errorResponse);
            throw new IllegalStateException("Failed to send OTP email. HTTP status: " + statusCode + " Response: " + errorResponse);
        }
    } catch (IOException exception) {
        throw new IllegalStateException("Failed to send OTP email", exception);
    }
}
    private String readInputStream(InputStream input) throws IOException {
        if (input == null) return "";
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}