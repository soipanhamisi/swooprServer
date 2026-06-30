package org.hamisi.swoopdserver.common;

import org.hamisi.swoopdserver.common.exceptions.InvalidTokenException;
import org.hamisi.swoopdserver.common.exceptions.TokenServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TokenManagementService {

    @Value("${JWT_SALT}")
    private String saltString;

    public TokenManagementService() {
    }

    public String createToken(UUID userId, String email){
        if (saltString == null || saltString.trim().isEmpty()) {
            throw new IllegalStateException("Configuration Error: JWT_SALT environment variable is missing.");
        }

        // Setup Base64URL Encoder (without padding)
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        // Construct the Header JSON string
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String encodedHeader = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

        // Construct the Payload JSON string with email instead of fullName
        long nowInSeconds = System.currentTimeMillis() / 1000;
        long expInSeconds = nowInSeconds + 604800; // Token valid for 1 week

        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId.toString(),
                email.replace("\"", "\\\""), // Basic escaping for safety
                nowInSeconds,
                expInSeconds
        );
        String encodedPayload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Combine Header and Payload to create the data payload for signing
        String tokenData = encodedHeader + "." + encodedPayload;

        try {
            // Initialize HMAC-SHA256 crypto instance
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    saltString.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(secretKeySpec);

            // Generate the cryptographic signature bytes
            byte[] signatureBytes = hmacSha256.doFinal(tokenData.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = encoder.encodeToString(signatureBytes);

            // Compile the complete 3-part token (stateless JWT - no DB persistence)
            return tokenData + "." + encodedSignature;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT signature using native cryptography", e);
        }

    }
    public AccessRecord verifyToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw new InvalidTokenException("Token is missing");
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidTokenException("Invalid token format");
            }

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String providedSignature = parts[2];

            // Recompute signature to verify authenticity
            String tokenData = encodedHeader + "." + encodedPayload;

            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    saltString.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(secretKeySpec);

            byte[] expectedSignatureBytes =
                    hmacSha256.doFinal(tokenData.getBytes(StandardCharsets.UTF_8));

            String expectedSignature = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(expectedSignatureBytes);

            if (!expectedSignature.equals(providedSignature)) {
                throw new InvalidTokenException("Invalid token signature");
            }

            // Decode payload
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(encodedPayload),
                    StandardCharsets.UTF_8
            );

            // Extract claims from payload
            String userId = payloadJson.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
            String email = payloadJson.replaceAll(".*\"email\":\"([^\"]+)\".*", "$1");
            String expString = payloadJson.replaceAll(".*\"exp\":(\\d+).*", "$1");

            // Validate token expiration
            long exp = Long.parseLong(expString);
            long now = System.currentTimeMillis() / 1000;

            if (now > exp) {
                throw new InvalidTokenException("Token expired");
            }

            return new AccessRecord(userId, email);

        } catch (Exception e) {
            throw new TokenServiceException("Token verification failed: " + e.getMessage());
        }
    }
}
