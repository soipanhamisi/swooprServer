package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.auth.dtos.UserDTO;
import org.hamisi.swoopdserver.auth.services.RegistrationService;
import org.hamisi.swoopdserver.auth.services.SimpleAuthService;
import org.hamisi.swoopdserver.auth.dtos.LoginCredentials;
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


    public authController(RegistrationService registrationService, SimpleAuthService simpleAuthService) {
        this.registrationService = registrationService;
        this.simpleAuthService = simpleAuthService;
    }

    @PostMapping("/registerUser")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO newUser){
        try {
            registrationService.registerUser(newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body("UserDTO registered successfully!");
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginCredentials userCredentials){
        try {
            simpleAuthService.checkEmailPassword(userCredentials.getEmail(), userCredentials.getPassword());
            return ResponseEntity.ok("Login successful!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
