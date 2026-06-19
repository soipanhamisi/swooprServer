package org.hamisi.swoopdserver.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamisi.swoopdserver.auth.dtos.*;
import org.hamisi.swoopdserver.auth.services.RegistrationService;
import org.hamisi.swoopdserver.auth.services.TokenManagementService;
import org.hamisi.swoopdserver.auth.services.UserAuthenticationService;
import org.hamisi.swoopdserver.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(authController.class)
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private UserAuthenticationService userAuthenticationService;

    @MockBean
    private TokenManagementService tokenManagementService;

    private EmailDTO emailDTO;
    private AuthCredsDTO authCredsDTO;
    private UserDTO userDTO;
    private SampleDTO sampleDTO;
    private String validJwt;

    @BeforeEach
    void setUp() {
        emailDTO = new EmailDTO();
        emailDTO.setEmail("student@usiu.ac.ke");

        authCredsDTO = new AuthCredsDTO();
        authCredsDTO.setEmail("student@usiu.ac.ke");
        authCredsDTO.setOtp(123456);

        userDTO = new UserDTO();
        userDTO.setFullName("John Doe");
        userDTO.setEmail("student@usiu.ac.ke");
        userDTO.setRole(Role.CARPOOL_HOST);

        sampleDTO = new SampleDTO();
        sampleDTO.setMessage("test message");
        validJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiZW1haWwiOiJzdHVkZW50QHVzaXUuYWMua2UiLCJpYXQiOjE2MjQ3Njk2MDAsImV4cCI6OTk5OTk5OTk5OX0.test";
    }

    // ==================== OTP Tests ====================

    @Test
    @DisplayName("POST /auth/getOtp - Request OTP successfully")
    void testGetOtpSuccess() throws Exception {
        doNothing().when(userAuthenticationService).createOtp(emailDTO.getEmail());

        mockMvc.perform(post("/auth/getOtp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent"));

        verify(userAuthenticationService, times(1)).createOtp(emailDTO.getEmail());
    }

    @Test
    @DisplayName("POST /auth/getOtp - Handle invalid email")
    void testGetOtpWithInvalidEmail() throws Exception {
        EmailDTO invalidEmail = new EmailDTO();
        invalidEmail.setEmail("");

        doNothing().when(userAuthenticationService).createOtp("");

        mockMvc.perform(post("/auth/getOtp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/getOtp - Handle exception during OTP creation")
    void testGetOtpWithException() throws Exception {
        doThrow(new RuntimeException("Email service unavailable"))
                .when(userAuthenticationService).createOtp(any());

        mockMvc.perform(post("/auth/getOtp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(status().isOk()) // Controller doesn't catch this, but framework may
        ;
    }

    // ==================== OTP Verification Tests ====================

    @Test
    @DisplayName("POST /auth/authenticateUser - Verify OTP successfully")
    void testAuthenticateUserSuccess() throws Exception {
        when(userAuthenticationService.verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail()))
                .thenReturn(true);

        mockMvc.perform(post("/auth/authenticateUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authCredsDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("user authenticated"));

        verify(userAuthenticationService, times(1))
                .verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail());
    }

    @Test
    @DisplayName("POST /auth/authenticateUser - Fail OTP verification with wrong OTP")
    void testAuthenticateUserWrongOtp() throws Exception {
        when(userAuthenticationService.verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail()))
                .thenReturn(false);

        mockMvc.perform(post("/auth/authenticateUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authCredsDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("user not authenticated"));
    }

    // ==================== User Registration Tests ====================

    @Test
    @DisplayName("POST /auth/saveUser - Register new user successfully")
    void testRegisterUserSuccess() throws Exception {
        String expectedJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1YjIyZDVjMy1jYjc1LTQ1ZTItOWYyNS03ZjJjYjc1ZGI2YjAiLCJlbWFpbCI6InN0dWRlbnRAcmJjLmFjLmtlIiwiaWF0IjoxNjI0NzY5NjAwLCJleHAiOjE2MjQ3NzMyMDB9.signature";

        when(registrationService.registerUser(any(UserDTO.class)))
                .thenReturn(expectedJwt);

        mockMvc.perform(post("/auth/saveUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().string(expectedJwt));

        verify(registrationService, times(1)).registerUser(any(UserDTO.class));
    }

    @Test
    @DisplayName("POST /auth/saveUser - User already exists")
    void testRegisterUserAlreadyExists() throws Exception {
        when(registrationService.registerUser(any(UserDTO.class)))
                .thenThrow(new RuntimeException("User already exists"));

        mockMvc.perform(post("/auth/saveUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User already exists"));
    }

    @Test
    @DisplayName("POST /auth/saveUser - Invalid user data")
    void testRegisterUserInvalidData() throws Exception {
        UserDTO invalidUser = new UserDTO();
        invalidUser.setFullName("");
        invalidUser.setEmail("");

        when(registrationService.registerUser(any(UserDTO.class)))
                .thenThrow(new RuntimeException("Invalid user data"));

        mockMvc.perform(post("/auth/saveUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid user data"));
    }

    // ==================== Token Generation Tests ====================

    @Test
    @DisplayName("POST /auth/getNewToken - Generate new token after OTP verification")
    void testGetNewTokenSuccess() throws Exception {
        String newToken = "eyJhbGciOiJIUzI1NiJ9.newPayload.newSignature";

        when(userAuthenticationService.verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail()))
                .thenReturn(true);
        when(userAuthenticationService.getNewToken(authCredsDTO.getEmail()))
                .thenReturn(newToken);

        mockMvc.perform(post("/auth/getNewToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authCredsDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string(newToken));

        verify(userAuthenticationService, times(1))
                .verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail());
        verify(userAuthenticationService, times(1))
                .getNewToken(authCredsDTO.getEmail());
    }

    @Test
    @DisplayName("POST /auth/getNewToken - Fail when OTP verification fails")
    void testGetNewTokenOtpVerificationFails() throws Exception {
        when(userAuthenticationService.verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail()))
                .thenReturn(false);

        mockMvc.perform(post("/auth/getNewToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authCredsDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("user not authenticated"));

        verify(userAuthenticationService, times(1))
                .verifyOtp(authCredsDTO.getOtp(), authCredsDTO.getEmail());
        verify(userAuthenticationService, never())
                .getNewToken(any());
    }

    // ==================== Token Verification Tests ====================

    @Test
    @DisplayName("POST /auth/testEndpoint - Verify valid JWT token")
    void testTokenVerificationSuccess() throws Exception {
        AccessRecord accessRecord = new AccessRecord(
                UUID.randomUUID().toString(),
                "student@usiu.ac.ke"
        );

        sampleDTO.setJwt(validJwt);

        when(tokenManagementService.verifyToken(validJwt))
                .thenReturn(accessRecord);

        mockMvc.perform(post("/auth/testEndpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello! student@usiu.ac.ke"));

        verify(tokenManagementService, times(1)).verifyToken(validJwt);
    }

    @Test
    @DisplayName("POST /auth/testEndpoint - Reject invalid JWT token")
    void testTokenVerificationInvalidToken() throws Exception {
        sampleDTO.setJwt("invalid.token.here");

        when(tokenManagementService.verifyToken("invalid.token.here"))
                .thenThrow(new RuntimeException("Invalid token signature"));

        mockMvc.perform(post("/auth/testEndpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid token signature"));
    }

    @Test
    @DisplayName("POST /auth/testEndpoint - Reject expired JWT token")
    void testTokenVerificationExpiredToken() throws Exception {
        sampleDTO.setJwt("expired.jwt.token");

        when(tokenManagementService.verifyToken("expired.jwt.token"))
                .thenThrow(new RuntimeException("Token expired"));

        mockMvc.perform(post("/auth/testEndpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token expired"));
    }

    @Test
    @DisplayName("POST /auth/testEndpoint - Reject null JWT token")
    void testTokenVerificationNullToken() throws Exception {
        sampleDTO.setJwt(null);

        when(tokenManagementService.verifyToken(null))
                .thenThrow(new RuntimeException("Token is missing"));

        mockMvc.perform(post("/auth/testEndpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token is missing"));
    }

}

