package org.hamisi.swoopdserver.jwtUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TokenManagementService {

    private final TokenRepository tokenRepository;
    @Value("${JWT_SALT}")
    private String saltString;

    public TokenManagementService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String createToken(UUID userId, String fullName){
        if (saltString == null || saltString.trim().isEmpty()) {
            throw new IllegalStateException("Configuration Error: JWT_SALT environment variable is missing.");
        }

        // 2. Setup Base64URL Encoder (without padding)
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        // 3. Construct the Header JSON string
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String encodedHeader = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

        // 4. Construct the Payload JSON string
        long nowInSeconds = System.currentTimeMillis() / 1000;
        long expInSeconds = nowInSeconds + 3600; // Token valid for 1 hour

        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"name\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId.toString(),
                fullName.replace("\"", "\\\""), // Basic escaping for safety
                nowInSeconds,
                expInSeconds
        );
        String encodedPayload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // 5. Combine Header and Payload to create the data payload for signing
        String tokenData = encodedHeader + "." + encodedPayload;

        try {
            // 6. Initialize HMAC-SHA256 crypto instance
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    saltString.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(secretKeySpec);

            // 7. Generate the cryptographic signature bytes
            byte[] signatureBytes = hmacSha256.doFinal(tokenData.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = encoder.encodeToString(signatureBytes);

            // 8. Compile the complete 3-part token
            String tokenString = tokenData + "." + encodedSignature;
            tokenRepository.save(new Token(tokenString));
            return tokenString;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT signature using native cryptography", e);
        }
    }
    public boolean isValidToken(String token) {
        return tokenRepository.existsByToken(token);
    }
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        long deletedCount = tokenRepository.deleteByExpiresAtBefore(oneWeekAgo);
        System.out.println("Deleted " + deletedCount + " expired tokens");
    }
}
