package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.auth.dtos.*;
import org.hamisi.swoopdserver.auth.services.RegistrationService;
import org.hamisi.swoopdserver.auth.services.TokenManagementService;
import org.hamisi.swoopdserver.auth.services.UserAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class authController  {

    private final RegistrationService registrationService;
    private final UserAuthenticationService userAuthenticationService;
    private final TokenManagementService tokenManagementService;

    public authController(RegistrationService registrationService, UserAuthenticationService userAuthenticationService, TokenManagementService tokenManagementService) {
        this.registrationService = registrationService;
        this.userAuthenticationService = userAuthenticationService;
        this.tokenManagementService = tokenManagementService;
    }

    /**
     *
     * @param email
     * @return http Ok message if otp is sent to email
     */
    @PostMapping("getOtp")
    public ResponseEntity<String> getOtp(@RequestBody EmailDTO email){
        userAuthenticationService.createOtp(email.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body("OTP sent");
    }

    /**
     *
     * @param authCreds = {email, otp}
     * @return http Ok message if user is authenticated
     */
    @PostMapping("authenticateUser")
    public ResponseEntity<String> authenticateUser(@RequestBody AuthCredsDTO authCreds){
        if (userAuthenticationService.verifyOtp(authCreds.getOtp(), authCreds.getEmail())){
            return ResponseEntity.status(HttpStatus.OK).body("user authenticated");
        }else
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("user not authenticated");
    }

    /**
     *
     * @param newUser description
     * @return jwt token
     */
    @PostMapping("/saveUser")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO newUser){
        try {
            String jwtToken = registrationService.registerUser(newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(jwtToken);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    /**
     * Meant to  be hit after /getOtp endpoint
     * @param authCreds = {email, otp}
     * @return jwt token
     */
    @PostMapping("getNewToken")
    public ResponseEntity<String> getNewToken(@RequestBody AuthCredsDTO authCreds){
        if(userAuthenticationService.verifyOtp(authCreds.getOtp(), authCreds.getEmail())){
         String newToken = userAuthenticationService.getNewToken(authCreds.getEmail());
         return ResponseEntity.status(HttpStatus.OK).body(newToken);
        }else 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("user not authenticated");
    }

    @PostMapping("/testEndpoint")
    public ResponseEntity<String> testEndpoint(@RequestBody SampleDTO sampleDTO){
        try {
            AccessRecord accessRecord = tokenManagementService.verifyToken(sampleDTO.getJwt());
            return ResponseEntity.status(HttpStatus.OK).body("Hello! " + accessRecord.getEmail());
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
