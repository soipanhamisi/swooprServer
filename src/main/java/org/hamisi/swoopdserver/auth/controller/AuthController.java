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
     *
     * @return http Ok message if otp is sent to email
     */
    @PostMapping("getOtp")
    public ResponseEntity<ApiResponse<Void>> getOtp(@RequestBody EmailDTO email){
        userAuthenticationService.createOtp(email.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Otp Sent"));
    }

    /**
     *
     * @param authCreds = {email, otp}
     * @return http Ok message if user is authenticated
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
     *
     * @param newUser description
     * @return jwt token
     */
    @PostMapping("/saveUser")
    public ResponseEntity<ApiResponse<Void>> registerUser(@RequestBody UserDTO newUser){
        String jwtToken = registrationService.registerUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).header("Authorization", "Bearer " + jwtToken).body(ApiResponse.success("User saved"));
    }


    @PostMapping("/testEndpoint")
    public ResponseEntity<ApiResponse<Void>> testEndpoint(
            @RequestHeader("Authorization") String authHeader
    ){
        AccessRecord accessRecord = tokenManagementService.verifyToken(authHeader);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Hello! " + accessRecord.getEmail()));
}

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
