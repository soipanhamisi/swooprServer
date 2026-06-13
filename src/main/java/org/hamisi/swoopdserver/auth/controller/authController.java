package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.auth.dtos.*;
import org.hamisi.swoopdserver.auth.services.RegistrationService;
import org.hamisi.swoopdserver.auth.services.SimpleAuthService;
import org.hamisi.swoopdserver.auth.services.UserAuthenticationService;
import org.hamisi.swoopdserver.jwtUtils.TokenManagementService;
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
    private final SimpleAuthService simpleAuthService;
    private final TokenManagementService tokenManagementService;
    private final UserAuthenticationService userAuthenticationService;


    public authController(RegistrationService registrationService, SimpleAuthService simpleAuthService, TokenManagementService tokenManagementService, UserAuthenticationService userAuthenticationService) {
        this.registrationService = registrationService;
        this.simpleAuthService = simpleAuthService;
        this.tokenManagementService = tokenManagementService;
        this.userAuthenticationService = userAuthenticationService;
    }

    @PostMapping("getOtp")
    public ResponseEntity<String> getOtp(@RequestBody EmailDTO email){
        userAuthenticationService.createOtp(email.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body("OTP sent");
    }

    @PostMapping("authenticateUser")
    public ResponseEntity<String> authenticateUser(@RequestBody AuthCredsDTO authCreds){
        if (userAuthenticationService.verifyOtp(authCreds.getOtp(), authCreds.getEmail())){
            return ResponseEntity.status(HttpStatus.OK).body("user authenticated");
        }else
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("user not authenticated");
    }

    @PostMapping("/registerUser")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO newUser){
        try {
            String jwtToken = registrationService.registerUser(newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(jwtToken);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/testEndpoint")
    public ResponseEntity<String> testEndpoint(@RequestBody SampleDTO sampleDTO){
        if (tokenManagementService.isValidToken(sampleDTO.getJwt())){
            return ResponseEntity.ok(sampleDTO.getMessage());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginCredentials userCredentials){
        try {
            String jwtToken = simpleAuthService.checkEmailPassword(userCredentials.getEmail(), userCredentials.getPassword());
            return ResponseEntity.status(HttpStatus.OK).body(jwtToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

}
