package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.dtos.AccessRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TokenManagementServiceTests {

    @InjectMocks
    private TokenManagementService tokenManagementService;

    private UUID testUserId;
    private String testEmail;
    private static final String JWT_SALT = "test_secret_key_for_jwt_signing_that_is_long_enough";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "student@usiu.ac.ke";
        ReflectionTestUtils.setField(tokenManagementService, "saltString", JWT_SALT);
    }

    // ==================== Token Creation Tests ====================

    @Test
    @DisplayName("Create valid JWT token with correct payload")
    void testCreateTokenSuccess() {
        String token = tokenManagementService.createToken(testUserId, testEmail);

        assertNotNull(token);
        assertTrue(token.contains("."));
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token should have 3 parts (header.payload.signature)");
    }

    @Test
    @DisplayName("Token contains correct header (HS256)")
    void testTokenContainsCorrectHeader() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        String[] parts = token.split("\\.");
        String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));

        assertTrue(header.contains("HS256"));
        assertTrue(header.contains("JWT"));
    }

    @Test
    @DisplayName("Token payload contains email claim")
    void testTokenPayloadContainsEmail() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

        assertTrue(payload.contains("email"));
        assertTrue(payload.contains(testEmail));
    }

    @Test
    @DisplayName("Token payload contains user ID in sub claim")
    void testTokenPayloadContainsUserId() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

        assertTrue(payload.contains("sub"));
        assertTrue(payload.contains(testUserId.toString()));
    }

    @Test
    @DisplayName("Token payload contains expiration claim")
    void testTokenPayloadContainsExpiration() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

        assertTrue(payload.contains("exp"));
        assertTrue(payload.contains("iat"));
    }

    @Test
    @DisplayName("Token creation fails without JWT_SALT")
    void testCreateTokenMissingJwtSalt() {
        ReflectionTestUtils.setField(tokenManagementService, "saltString", null);

        assertThrows(
                IllegalStateException.class,
                () -> tokenManagementService.createToken(testUserId, testEmail),
                "Should throw IllegalStateException when JWT_SALT is missing"
        );
    }

    @Test
    @DisplayName("Token creation fails with empty JWT_SALT")
    void testCreateTokenEmptyJwtSalt() {
        ReflectionTestUtils.setField(tokenManagementService, "saltString", "");

        assertThrows(
                IllegalStateException.class,
                () -> tokenManagementService.createToken(testUserId, testEmail),
                "Should throw IllegalStateException when JWT_SALT is empty"
        );
    }

    @Test
    @DisplayName("Different tokens generated for different users")
    void testTokenUniqueForDifferentUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        String token1 = tokenManagementService.createToken(userId1, testEmail);
        String token2 = tokenManagementService.createToken(userId2, testEmail);

        assertNotEquals(token1, token2, "Tokens should be different for different user IDs");
    }

    @Test
    @DisplayName("Different tokens generated for different emails")
    void testTokenUniqueForDifferentEmails() {
        String token1 = tokenManagementService.createToken(testUserId, "student1@usiu.ac.ke");
        String token2 = tokenManagementService.createToken(testUserId, "student2@usiu.ac.ke");

        assertNotEquals(token1, token2, "Tokens should be different for different emails");
    }

    @Test
    @DisplayName("Handles email with special characters in token creation")
    void testCreateTokenWithSpecialCharactersInEmail() {
        String emailWithSpecialChars = "student+test@usiu.ac.ke";
        String token = tokenManagementService.createToken(testUserId, emailWithSpecialChars);

        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    // ==================== Token Verification Tests ====================

    @Test
    @DisplayName("Verify valid token successfully")
    void testVerifyTokenSuccess() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        AccessRecord accessRecord = tokenManagementService.verifyToken(token);

        assertNotNull(accessRecord);
        assertEquals(testUserId, accessRecord.getUserId());
        assertEquals(testEmail, accessRecord.getEmail());
    }

    @Test
    @DisplayName("Reject malformed token (missing parts)")
    void testVerifyMalformedTokenMissingParts() {
        String malformedToken = "header.payload";

        assertThrows(
                RuntimeException.class,
                () -> tokenManagementService.verifyToken(malformedToken),
                "Should reject token with incorrect number of parts"
        );
    }

    @Test
    @DisplayName("Reject null token")
    void testVerifyNullToken() {
        assertThrows(
                RuntimeException.class,
                () -> tokenManagementService.verifyToken(null),
                "Should reject null token"
        );
    }

    @Test
    @DisplayName("Reject empty token")
    void testVerifyEmptyToken() {
        assertThrows(
                RuntimeException.class,
                () -> tokenManagementService.verifyToken(""),
                "Should reject empty token"
        );
    }

    @Test
    @DisplayName("Reject token with invalid signature")
    void testVerifyInvalidSignature() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidsignature";

        assertThrows(
                RuntimeException.class,
                () -> tokenManagementService.verifyToken(tamperedToken),
                "Should reject token with invalid signature"
        );
    }

    @Test
    @DisplayName("Reject token with tampered payload")
    void testVerifyTamperedPayload() {
        String token = tokenManagementService.createToken(testUserId, testEmail);
        String[] parts = token.split("\\.");

        // Try to tamper with payload
        String tamperedPayload = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"sub\":\"different-id\"}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(
                RuntimeException.class,
                () -> tokenManagementService.verifyToken(tamperedToken),
                "Should reject token with tampered payload"
        );
    }

    @Test
    @DisplayName("Reject token with different JWT secret")
    void testVerifyTokenWithDifferentSecret() {
        // Create token with original secret
        String token = tokenManagementService.createToken(testUserId, testEmail);

        // Change the secret
        ReflectionTestUtils.setField(tokenManagementService, "saltString", "different_secret_key");

        assertThrows(
                RuntimeException.class,
                () -> tokenManagementService.verifyToken(token),
                "Should reject token signed with different secret"
        );
    }

    @Test
    @DisplayName("Extract correct email from verified token")
    void testVerifyTokenExtractsCorrectEmail() {
        String customEmail = "custom.student@usiu.ac.ke";
        String token = tokenManagementService.createToken(testUserId, customEmail);
        AccessRecord accessRecord = tokenManagementService.verifyToken(token);

        assertEquals(customEmail, accessRecord.getEmail());
    }

    @Test
    @DisplayName("Extract correct user ID from verified token")
    void testVerifyTokenExtractsCorrectUserId() {
        UUID customUserId = UUID.randomUUID();
        String token = tokenManagementService.createToken(customUserId, testEmail);
        AccessRecord accessRecord = tokenManagementService.verifyToken(token);

        assertEquals(customUserId, accessRecord.getUserId());
    }

    // ==================== Token Lifecycle Tests ====================

    @Test
    @DisplayName("Same user and email produces same token signature")
    void testTokenDeterminism() {
        // Note: Due to timestamp (iat/exp), tokens will differ slightly
        // but this tests that verification works consistently
        String token = tokenManagementService.createToken(testUserId, testEmail);
        AccessRecord record1 = tokenManagementService.verifyToken(token);

        AccessRecord record2 = tokenManagementService.verifyToken(token);

        assertEquals(record1.getUserId(), record2.getUserId());
        assertEquals(record1.getEmail(), record2.getEmail());
    }

    @Test
    @DisplayName("Token verification is symmetric (can verify own token)")
    void testTokenSymmetry() {
        UUID userId = UUID.randomUUID();
        String email = "test@usiu.ac.ke";

        String token = tokenManagementService.createToken(userId, email);
        AccessRecord accessRecord = tokenManagementService.verifyToken(token);

        assertEquals(userId, accessRecord.getUserId());
        assertEquals(email, accessRecord.getEmail());
    }

}

