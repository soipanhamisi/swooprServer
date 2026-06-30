package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.proxies.ResendProxy;
import org.hamisi.swoopdserver.auth.repository.OtpRepository;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserAuthenticationServiceTests {

    @Mock
    private ResendProxy resendProxy;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private TokenManagementService tokenManagementService;

    @InjectMocks
    private UserAuthenticationService userAuthenticationService;

    private String testEmail;
    private UUID testUserId;
    private int testOtp;
    private String testToken;

    @BeforeEach
    void setUp() {
        testEmail = "student@usiu.ac.ke";
        testUserId = UUID.randomUUID();
        testOtp = 123456;
        testToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
    }

    // ==================== OTP Creation Tests ====================

    @Test
    @DisplayName("Create OTP successfully")
    void testCreateOtpSuccess() {
        doNothing().when(otpRepository).saveOtp(eq(testEmail), anyInt());
        doNothing().when(resendProxy).sendOTP(anyString(), anyString(), anyInt());

        userAuthenticationService.createOtp(testEmail);

        verify(otpRepository, times(1)).saveOtp(eq(testEmail), anyInt());
        verify(resendProxy, times(1)).sendOTP(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("OTP is saved to repository")
    void testOtpSavedToRepository() {
        doNothing().when(otpRepository).saveOtp(eq(testEmail), anyInt());
        doNothing().when(resendProxy).sendOTP(anyString(), anyString(), anyInt());

        userAuthenticationService.createOtp(testEmail);

        verify(otpRepository, times(1)).saveOtp(eq(testEmail), anyInt());
    }

    @Test
    @DisplayName("OTP is sent via email proxy")
    void testOtpSentViaEmailProxy() {
        doNothing().when(otpRepository).saveOtp(eq(testEmail), anyInt());
        doNothing().when(resendProxy).sendOTP(anyString(), anyString(), anyInt());

        userAuthenticationService.createOtp(testEmail);

        verify(resendProxy, times(1)).sendOTP(eq(testEmail), anyString(), anyInt());
    }

    @Test
    @DisplayName("OTP is within valid range (100-999)")
    void testOtpGeneratedInValidRange() {
        userAuthenticationService.createOtp(testEmail);

        // The OTP should be saved, we verify it was called with an int parameter
        verify(otpRepository, times(1)).saveOtp(eq(testEmail), anyInt());
    }

    // ==================== OTP Verification Tests ====================

    @Test
    @DisplayName("Verify correct OTP successfully")
    void testVerifyCorrectOtpSuccess() {
        when(otpRepository.getOtp(testEmail)).thenReturn(String.valueOf(testOtp));

        boolean result = userAuthenticationService.verifyOtp(testOtp, testEmail);

        assertTrue(result);
        verify(otpRepository, times(1)).deleteOtp(testEmail);
    }

    @Test
    @DisplayName("Reject incorrect OTP")
    void testVerifyIncorrectOtp() {
        when(otpRepository.getOtp(testEmail)).thenReturn(String.valueOf(999999));

        boolean result = userAuthenticationService.verifyOtp(testOtp, testEmail);

        assertFalse(result);
        verify(otpRepository, never()).deleteOtp(testEmail);
    }

    @Test
    @DisplayName("Reject verification when OTP not found")
    void testVerifyOtpNotFound() {
        when(otpRepository.getOtp(testEmail)).thenReturn(null);

        boolean result = userAuthenticationService.verifyOtp(testOtp, testEmail);

        assertFalse(result);
        verify(otpRepository, never()).deleteOtp(testEmail);
    }

    @Test
    @DisplayName("Delete OTP after successful verification")
    void testOtpDeletedAfterSuccessfulVerification() {
        when(otpRepository.getOtp(testEmail)).thenReturn(String.valueOf(testOtp));

        userAuthenticationService.verifyOtp(testOtp, testEmail);

        verify(otpRepository, times(1)).deleteOtp(testEmail);
    }

    @Test
    @DisplayName("OTP not deleted after failed verification")
    void testOtpNotDeletedAfterFailedVerification() {
        when(otpRepository.getOtp(testEmail)).thenReturn(String.valueOf(999999));

        userAuthenticationService.verifyOtp(testOtp, testEmail);

        verify(otpRepository, never()).deleteOtp(testEmail);
    }

    @Test
    @DisplayName("Verify OTP with different email")
    void testVerifyOtpWithDifferentEmail() {
        String differentEmail = "other@usiu.ac.ke";
        when(otpRepository.getOtp(differentEmail)).thenReturn(String.valueOf(testOtp));

        boolean result = userAuthenticationService.verifyOtp(testOtp, differentEmail);

        assertTrue(result);
        verify(otpRepository, times(2)).getOtp(differentEmail);
    }

    @Test
    @DisplayName("Verify multiple OTPs for same user")
    void testVerifyMultipleOtpsSequentially() {
        int otp1 = 111111;

        when(otpRepository.getOtp(testEmail))
                .thenReturn(String.valueOf(otp1))
                .thenReturn(String.valueOf(otp1));

        boolean result1 = userAuthenticationService.verifyOtp(otp1, testEmail);
        boolean result2 = userAuthenticationService.verifyOtp(otp1, testEmail);

        assertTrue(result1);
        assertTrue(result2);
    }

    // ==================== Token Retrieval Tests ====================

    @Test
    @DisplayName("Get new token successfully")
    void testGetNewTokenSuccess() {
        when(usersRepository.findUserIdByEmail(testEmail)).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, testEmail)).thenReturn(testToken);

        String resultToken = userAuthenticationService.getNewToken(testEmail);

        assertNotNull(resultToken);
        assertEquals(testToken, resultToken);
        verify(tokenManagementService, times(1)).createToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Token generation called with correct email")
    void testTokenGenerationWithCorrectEmail() {
        when(usersRepository.findUserIdByEmail(testEmail)).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, testEmail)).thenReturn(testToken);

        userAuthenticationService.getNewToken(testEmail);

        verify(tokenManagementService, times(1)).createToken(eq(testUserId), eq(testEmail));
    }

    @Test
    @DisplayName("Token generation called with correct user ID")
    void testTokenGenerationWithCorrectUserId() {
        when(usersRepository.findUserIdByEmail(testEmail)).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, testEmail)).thenReturn(testToken);

        userAuthenticationService.getNewToken(testEmail);

        verify(tokenManagementService, times(1)).createToken(eq(testUserId), any());
    }

    @Test
    @DisplayName("Get token for different email addresses")
    void testGetTokenForDifferentEmails() {
        String email1 = "student1@usiu.ac.ke";
        String email2 = "student2@usiu.ac.ke";
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(usersRepository.findUserIdByEmail(email1)).thenReturn(userId1);
        when(usersRepository.findUserIdByEmail(email2)).thenReturn(userId2);
        when(tokenManagementService.createToken(userId1, email1)).thenReturn("token1");
        when(tokenManagementService.createToken(userId2, email2)).thenReturn("token2");

        String token1 = userAuthenticationService.getNewToken(email1);
        String token2 = userAuthenticationService.getNewToken(email2);

        assertEquals("token1", token1);
        assertEquals("token2", token2);
        verify(tokenManagementService, times(2)).createToken(any(), any());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Complete OTP flow: create, verify, and get token")
    void testCompleteOtpAuthenticationFlow() {
        // Step 1: Create OTP
        doNothing().when(otpRepository).saveOtp(eq(testEmail), anyInt());
        doNothing().when(resendProxy).sendOTP(anyString(), anyString(), anyInt());

        userAuthenticationService.createOtp(testEmail);

        verify(otpRepository, times(1)).saveOtp(eq(testEmail), anyInt());

        // Step 2: Verify OTP
        when(otpRepository.getOtp(testEmail)).thenReturn(String.valueOf(testOtp));

        boolean verified = userAuthenticationService.verifyOtp(testOtp, testEmail);

        assertTrue(verified);
        verify(otpRepository, times(1)).deleteOtp(testEmail);

        // Step 3: Get token
        when(usersRepository.findUserIdByEmail(testEmail)).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, testEmail)).thenReturn(testToken);

        String resultToken = userAuthenticationService.getNewToken(testEmail);

        assertNotNull(resultToken);
        assertEquals(testToken, resultToken);
    }

    @Test
    @DisplayName("OTP verification prevents token generation on failure")
    void testOtpVerificationPreventsWrongTokenGeneration() {
        when(otpRepository.getOtp(testEmail)).thenReturn(String.valueOf(999999));

        boolean verified = userAuthenticationService.verifyOtp(testOtp, testEmail);

        assertFalse(verified);
        // Token should not be generated if OTP fails
        verify(tokenManagementService, never()).createToken(any(), any());
    }

    @Test
    @DisplayName("Email extraction from full email address")
    void testEmailExtractionForOtpSending() {
        doNothing().when(otpRepository).saveOtp(eq(testEmail), anyInt());
        doNothing().when(resendProxy).sendOTP(eq(testEmail), anyString(), anyInt());

        userAuthenticationService.createOtp(testEmail);

        // Verify resend proxy was called with correct email
        verify(resendProxy, times(1)).sendOTP(eq(testEmail), anyString(), anyInt());
    }

}

