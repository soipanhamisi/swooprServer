package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.dtos.UserDTO;
import org.hamisi.swoopdserver.auth.exceptions.UserExistsException;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.users.Role;
import org.hamisi.swoopdserver.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTests {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private TokenManagementService tokenManagementService;

    @InjectMocks
    private RegistrationService registrationService;

    private UserDTO userDTO;
    private UUID testUserId;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

        userDTO = new UserDTO();
        userDTO.setFullName("John Doe");
        userDTO.setEmail("student@usiu.ac.ke");
        userDTO.setRole(Role.CARPOOL_HOST);
    }

    // ==================== Successful Registration Tests ====================

    @Test
    @DisplayName("Register new user successfully")
    void testRegisterUserSuccess() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(usersRepository.findUserIdByEmail(userDTO.getEmail())).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, userDTO.getEmail())).thenReturn(testToken);

        String resultToken = registrationService.registerUser(userDTO);

        assertNotNull(resultToken);
        assertEquals(testToken, resultToken);
        verify(usersRepository, times(1)).addUser(any(User.class));
        verify(tokenManagementService, times(1)).createToken(testUserId, userDTO.getEmail());
    }

    @Test
    @DisplayName("Verify user entity is created with correct data")
    void testRegisterUserCreatesCorrectEntity() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(usersRepository.findUserIdByEmail(userDTO.getEmail())).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, userDTO.getEmail())).thenReturn(testToken);

        registrationService.registerUser(userDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(usersRepository, times(1)).addUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(userDTO.getFullName(), capturedUser.getFullName());
        assertEquals(userDTO.getEmail(), capturedUser.getEmail());
        assertEquals(userDTO.getRole(), capturedUser.getRole());
    }

    @Test
    @DisplayName("Return JWT token after successful registration")
    void testRegisterUserReturnsToken() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(usersRepository.findUserIdByEmail(userDTO.getEmail())).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, userDTO.getEmail())).thenReturn(testToken);

        String resultToken = registrationService.registerUser(userDTO);

        assertNotNull(resultToken);
        assertTrue(resultToken.contains("."));
    }

    // ==================== Duplicate User Tests ====================

    @Test
    @DisplayName("Reject registration for existing user")
    void testRegisterExistingUserThrowsException() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(true);

        assertThrows(UserExistsException.class, () -> registrationService.registerUser(userDTO));
        verify(usersRepository, never()).addUser(any());
        verify(tokenManagementService, never()).createToken(any(), any());
    }

    @Test
    @DisplayName("User already exists exception contains correct message")
    void testUserExistsExceptionMessage() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(true);

        UserExistsException exception = assertThrows(
                UserExistsException.class,
                () -> registrationService.registerUser(userDTO)
        );

        assertTrue(exception.getMessage().contains("User already exists"));
    }

    // ==================== User Data Validation Tests ====================

    @Test
    @DisplayName("Register user with different roles")
    void testRegisterUserWithDifferentRoles() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(usersRepository.findUserIdByEmail(userDTO.getEmail())).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, userDTO.getEmail())).thenReturn(testToken);

        // Test CARPOOL_HOST
        userDTO.setRole(Role.CARPOOL_HOST);
        registrationService.registerUser(userDTO);

        // Test RIDE_SEEKER
        userDTO.setRole(Role.RIDE_SEEKER);
        registrationService.registerUser(userDTO);

        verify(usersRepository, times(2)).addUser(any(User.class));
    }

    @Test
    @DisplayName("Register multiple different users")
    void testRegisterMultipleDifferentUsers() {
        // First user
        when(usersRepository.existsByEmail("student1@usiu.ac.ke")).thenReturn(false);
        UUID userId1 = UUID.randomUUID();
        when(usersRepository.findUserIdByEmail("student1@usiu.ac.ke")).thenReturn(userId1);
        when(tokenManagementService.createToken(userId1, "student1@usiu.ac.ke"))
                .thenReturn("token1");

        UserDTO user1 = new UserDTO();
        user1.setFullName("Student One");
        user1.setEmail("student1@usiu.ac.ke");
        user1.setRole(Role.RIDE_SEEKER);

        String token1 = registrationService.registerUser(user1);
        assertEquals("token1", token1);

        // Second user
        when(usersRepository.existsByEmail("student2@usiu.ac.ke")).thenReturn(false);
        UUID userId2 = UUID.randomUUID();
        when(usersRepository.findUserIdByEmail("student2@usiu.ac.ke")).thenReturn(userId2);
        when(tokenManagementService.createToken(userId2, "student2@usiu.ac.ke"))
                .thenReturn("token2");

        UserDTO user2 = new UserDTO();
        user2.setFullName("Student Two");
        user2.setEmail("student2@usiu.ac.ke");
        user2.setRole(Role.CARPOOL_HOST);

        String token2 = registrationService.registerUser(user2);
        assertEquals("token2", token2);

        verify(usersRepository, times(2)).addUser(any(User.class));
    }

    // ==================== Token Generation Integration Tests ====================

    @Test
    @DisplayName("Token is generated with correct email parameter")
    void testTokenGeneratedWithCorrectEmail() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(usersRepository.findUserIdByEmail(userDTO.getEmail())).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, userDTO.getEmail())).thenReturn(testToken);

        registrationService.registerUser(userDTO);

        verify(tokenManagementService, times(1))
                .createToken(testUserId, userDTO.getEmail());
    }

    @Test
    @DisplayName("Token is generated with correct user ID parameter")
    void testTokenGeneratedWithCorrectUserId() {
        when(usersRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(usersRepository.findUserIdByEmail(userDTO.getEmail())).thenReturn(testUserId);
        when(tokenManagementService.createToken(testUserId, userDTO.getEmail())).thenReturn(testToken);

        registrationService.registerUser(userDTO);

        verify(tokenManagementService, times(1))
                .createToken(eq(testUserId), any());
    }

}

