package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.auth.dtos.*;
import org.hamisi.swoopdserver.auth.services.RegistrationService;
import org.hamisi.swoopdserver.common.AccessRecord;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.auth.services.UserAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication and onboarding endpoints for the Swoopd backend.
 *
 * <p>Unless otherwise noted, responses use the shared {@code ApiResponse} envelope:</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": null
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/auth")
public class AuthController  {

    private final RegistrationService registrationService;
    private final UserAuthenticationService userAuthenticationService;
    private final TokenManagementService tokenManagementService;

    public AuthController(RegistrationService registrationService, UserAuthenticationService userAuthenticationService, TokenManagementService tokenManagementService) {
        this.registrationService = registrationService;
        this.userAuthenticationService = userAuthenticationService;
        this.tokenManagementService = tokenManagementService;
    }

    /**
     * Sends an OTP to the supplied email address.
     *
     * <p>Inbound JSON ({@link EmailDTO}):</p>
     * <pre>{@code
     * {
     *   "email": "student@usiu.ac.ke"
     * }
     * }</pre>
     *
     * <p>Outbound JSON on success:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Otp Sent",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("getOtp")
    public ResponseEntity<ApiResponse<Void>> getOtp(@RequestBody EmailDTO email){
        userAuthenticationService.createOtp(email.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Otp Sent"));
    }

    /**
     * Verifies an OTP submitted by the user.
     *
     * <p>Inbound JSON ({@link EmailAuthCredsDTO}):</p>
     * <pre>{@code
     * {
     *   "email": "student@usiu.ac.ke",
     *   "otp": 123456
     * }
     * }</pre>
     *
     * <p>Outbound JSON on success:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "User Verified",
     *   "data": null
     * }
     * }</pre>
     *
     * <p>Outbound JSON on OTP mismatch:</p>
     * <pre>{@code
     * {
     *   "success": false,
     *   "message": "Wrong OTP",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("authenticateUser")
    public ResponseEntity<ApiResponse<Void>> authenticateUser(@RequestBody EmailAuthCredsDTO authCreds){
        if (userAuthenticationService.verifyOtp(authCreds.getOtp(), authCreds.getEmail())){
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("User Verified"));
        }else
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure("Wrong OTP"));
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<ApiResponse<Void>> getRefreshToken(
            @RequestHeader("Authorization") String authHeader
    ){
        AccessRecord accessRecord = tokenManagementService.extractUuidAndEmail(authHeader);
        String jwt = tokenManagementService.createToken(accessRecord.getUserId(), accessRecord.getEmail());
        return ResponseEntity.status(HttpStatus.OK).header("Authorization", "Bearer " + jwt).body(ApiResponse.success("Refresh Token Generated"));
    }

    /**
     * Registers a new user and returns a JWT in the response header.
     *
     * <p>Inbound JSON ({@link UserDTO}):</p>
     * <pre>{@code
     * {
     *   "fullName": "Jane Doe",
     *   "email": "student@usiu.ac.ke",
     *   "role": "NORMAL_USER",
     *   "messagingToken": "optional-fcm-token"
     * }
     * }</pre>
     *
     * <p>Outbound JSON body on success:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "User saved",
     *   "data": null
     * }
     * }</pre>
     *
     * <p>The generated JWT is returned in the {@code Authorization} response header as:</p>
     * <pre>{@code
     * Authorization: Bearer <jwt>
     * }</pre>
     */
    @PostMapping("/saveUser")
    public ResponseEntity<ApiResponse<Void>> registerUser(@RequestBody UserDTO newUser){
        String jwtToken = registrationService.registerUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).header("Authorization", "Bearer " + jwtToken).body(ApiResponse.success("User saved"));
    }


    /**
     * Lightweight authenticated test endpoint that echoes the caller email.
     *
     * <p>Inbound:</p>
     * <ul>
     *   <li>{@code Authorization} header with a bearer token</li>
     * </ul>
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Hello! student@usiu.ac.ke",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("/testEndpoint")
    public ResponseEntity<ApiResponse<Void>> testEndpoint(
            @RequestHeader("Authorization") String authHeader
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Hello! " + accessRecord.getEmail()));
}

    /**
     * Stores an FCM/messaging token for the authenticated user.
     *
     * <p>Inbound:</p>
     * <ul>
     *   <li>{@code Authorization} header with a bearer token</li>
     *   <li>Request body as a raw JSON string or plain text token value, for example:</li>
     * </ul>
     * <pre>{@code
     * "fcm-token-value"
     * }</pre>
     *
     * <p>Outbound JSON on success:</p>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Messaging Token Submitted",
     *   "data": null
     * }
     * }</pre>
     */
    @PostMapping("/submitMessagingToken")
    public ResponseEntity<ApiResponse<Void>> submitMessagingToken(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody String messagingToken
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(bearerToken);
        registrationService.setMessagingToken(messagingToken, accessRecord.getUserId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Messaging Token Submitted"));
    }
}
